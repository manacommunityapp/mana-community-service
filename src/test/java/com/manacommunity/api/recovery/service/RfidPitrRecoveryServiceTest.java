package com.manacommunity.api.recovery.service;

import com.manacommunity.api.recovery.dto.RecoveryDtos.*;
import com.manacommunity.api.recovery.entity.BulkMutationGuardrailEntity;
import com.manacommunity.api.recovery.entity.EmergencyGateOverrideEntity;
import com.manacommunity.api.recovery.entity.RfidTagSnapshotEntity;
import com.manacommunity.api.recovery.repository.BulkMutationGuardrailRepository;
import com.manacommunity.api.recovery.repository.EmergencyGateOverrideRepository;
import com.manacommunity.api.recovery.repository.RfidTagSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class RfidPitrRecoveryServiceTest {

    private RfidPitrRecoveryService createService(
            Map<String, RfidTagSnapshotEntity> tags,
            Map<String, EmergencyGateOverrideEntity> overrides,
            List<BulkMutationGuardrailEntity> guardrails) {
        RfidTagSnapshotRepository tagRepo = Mockito.mock(RfidTagSnapshotRepository.class);
        EmergencyGateOverrideRepository ovRepo = Mockito.mock(EmergencyGateOverrideRepository.class);
        BulkMutationGuardrailRepository guardRepo = Mockito.mock(BulkMutationGuardrailRepository.class);

        when(tagRepo.save(any())).thenAnswer(inv -> {
            RfidTagSnapshotEntity t = inv.getArgument(0);
            tags.put(t.getTagId(), t);
            return t;
        });
        when(tagRepo.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(tags.get(inv.getArgument(0))));
        when(tagRepo.findBySocietyIdOrderByTagIdAsc(anyLong())).thenAnswer(inv -> {
            long sid = inv.getArgument(0);
            return tags.values().stream()
                    .filter(t -> t.getSocietyId() == sid)
                    .sorted(Comparator.comparing(RfidTagSnapshotEntity::getTagId))
                    .toList();
        });

        when(ovRepo.save(any())).thenAnswer(inv -> {
            EmergencyGateOverrideEntity e = inv.getArgument(0);
            overrides.put(e.getSocietyId() + ":" + e.getFeatureKey(), e);
            return e;
        });
        when(ovRepo.findBySocietyIdAndFeatureKey(anyLong(), anyString())).thenAnswer(inv ->
                Optional.ofNullable(overrides.get(inv.getArgument(0) + ":" + inv.getArgument(1))));

        when(guardRepo.save(any())).thenAnswer(inv -> {
            BulkMutationGuardrailEntity g = inv.getArgument(0);
            guardrails.add(g);
            return g;
        });

        return new RfidPitrRecoveryService(tagRepo, ovRepo, guardRepo);
    }

    @Test
    void mitigatesWithFailSoftOverrideThenSurgicallyRestoresViaPitrDiff() {
        Map<String, RfidTagSnapshotEntity> tags = new HashMap<>();
        Map<String, EmergencyGateOverrideEntity> overrides = new HashMap<>();
        List<BulkMutationGuardrailEntity> guardrails = new ArrayList<>();
        RfidPitrRecoveryService service = createService(tags, overrides, guardrails);

        Instant badPushTime = Instant.parse("2026-11-28T02:45:00Z"); // 08:15 IST
        service.simulate815AmCorruption(1L, badPushTime);

        // Before override -> blocked
        GateEvaluationDto blocked = service.evaluateGateEntry(1L, "RFID-1-0001");
        assertEquals(GateAccessDecision.DENY_INACTIVE, blocked.decision());

        // Turn on scoped emergency override -> opens road for residents, still blocks blacklist
        service.toggleEmergencyOverride(new EmergencyOverrideRequest(
                1L, true, Instant.parse("2026-11-28T02:44:00Z"), "ONCALL_SRE"
        ));
        GateEvaluationDto residentPass = service.evaluateGateEntry(1L, "RFID-1-0001");
        assertEquals(GateAccessDecision.PROVISIONAL_EMERGENCY_PASS, residentPass.decision());
        assertTrue(residentPass.emergencyOverrideApplied());

        GateEvaluationDto blacklisted = service.evaluateGateEntry(1L, "RFID-1-0010");
        assertEquals(GateAccessDecision.DENY_BLACKLISTED, blacklisted.decision());

        // Surgical PITR restore fixes 9 legitimate tags, skips 1 blacklisted, auto-clears override
        SurgicalPitrRestoreResultDto restored = service.executeSurgicalPitrRestore(
                1L, Instant.parse("2026-11-28T02:44:00Z"), "SRE_PITR"
        );
        assertEquals(9, restored.restoredCount());
        assertEquals(1, restored.skippedBlacklistedOrUntouched());
        assertTrue(restored.overrideAutoCleared());

        assertEquals(GateAccessDecision.ALLOW_ACTIVE, service.evaluateGateEntry(1L, "RFID-1-0001").decision());
        assertEquals(GateAccessDecision.DENY_BLACKLISTED, service.evaluateGateEntry(1L, "RFID-1-0010").decision());
    }

    @Test
    void enforcesRowCapAndCanaryAnomalyAbortGuardrails() {
        Map<String, RfidTagSnapshotEntity> tags = new HashMap<>();
        Map<String, EmergencyGateOverrideEntity> overrides = new HashMap<>();
        List<BulkMutationGuardrailEntity> guardrails = new ArrayList<>();
        RfidPitrRecoveryService service = createService(tags, overrides, guardrails);

        BulkMutationGuardrailDto rowCap = service.attemptGuardedBulkMutation(
                new BulkMutationCheckRequest(1L, "RFID_DEACTIVATE", 4000, 10, 0, 101L, null, "Nightly sync")
        );
        assertEquals(GuardrailDecision.BLOCKED_ROW_CAP, rowCap.decision());

        BulkMutationGuardrailDto canaryAbort = service.attemptGuardedBulkMutation(
                new BulkMutationCheckRequest(1L, "RFID_DEACTIVATE", 4000, 10, 14, 101L, 102L, "Canary spike")
        );
        assertEquals(GuardrailDecision.CANARY_ABORTED, canaryAbort.decision());

        BulkMutationGuardrailDto ok = service.attemptGuardedBulkMutation(
                new BulkMutationCheckRequest(1L, "RFID_DEACTIVATE", 4000, 10, 1, 101L, 102L, "Validated batch")
        );
        assertEquals(GuardrailDecision.APPROVED, ok.decision());
    }
}
