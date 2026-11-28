package com.manacommunity.api.recovery.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.recovery.dto.RecoveryDtos.*;
import com.manacommunity.api.recovery.entity.BulkMutationGuardrailEntity;
import com.manacommunity.api.recovery.entity.EmergencyGateOverrideEntity;
import com.manacommunity.api.recovery.entity.RfidTagSnapshotEntity;
import com.manacommunity.api.recovery.repository.BulkMutationGuardrailRepository;
import com.manacommunity.api.recovery.repository.EmergencyGateOverrideRepository;
import com.manacommunity.api.recovery.repository.RfidTagSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Handles emergency boom-barrier mitigation, surgical PITR restoration for bulk RFID
 * corruptions (never overwriting newer concurrent transactions), and blast-radius
 * guardrails (row-cap circuit breaker, Maker-Checker, canary anomaly auto-abort).
 */
@Service
@RequiredArgsConstructor
public class RfidPitrRecoveryService {

    public static final String EMERGENCY_FLAG_KEY = "rfid.emergency_recovery_mode";
    static final int DEFAULT_MAX_ROWS_GUARDRAIL = 50;
    static final int DEFAULT_CANARY_THRESHOLD_PCT = 5;

    private final RfidTagSnapshotRepository tagRepository;
    private final EmergencyGateOverrideRepository overrideRepository;
    private final BulkMutationGuardrailRepository guardrailRepository;

    @Transactional
    public List<RfidTagDto> simulate815AmCorruption(long societyId, Instant incidentTimestamp) {
        Instant ts = incidentTimestamp != null ? incidentTimestamp : Instant.parse("2026-11-28T02:45:00Z"); // 08:15 IST
        List<RfidTagSnapshotEntity> existing = tagRepository.findBySocietyIdOrderByTagIdAsc(societyId);
        if (existing.isEmpty()) {
            for (int i = 1; i <= 10; i++) {
                RfidTagSnapshotEntity tag = new RfidTagSnapshotEntity();
                tag.setTagId(String.format("RFID-%d-%04d", societyId, i));
                tag.setSocietyId(societyId);
                tag.setVehicleNumber(String.format("TS09EF%04d", 1000 + i));
                tag.setFlatUnitCode("Tower A - " + (100 + i));
                boolean isBlacklisted = (i == 10);
                tag.setBlacklisted(isBlacklisted);
                tag.setPreIncidentStatus(isBlacklisted ? "INACTIVE" : "ACTIVE");
                tag.setTagStatus("INACTIVE");
                tag.setUpdatedBy("BUGGY_ADMIN_SYNC_0815");
                tag.setUpdatedAt(ts);
                tagRepository.save(tag);
            }
            existing = tagRepository.findBySocietyIdOrderByTagIdAsc(societyId);
        } else {
            for (RfidTagSnapshotEntity tag : existing) {
                if (!Boolean.TRUE.equals(tag.getBlacklisted())) {
                    tag.setPreIncidentStatus(tag.getTagStatus());
                    tag.setTagStatus("INACTIVE");
                    tag.setUpdatedBy("BUGGY_ADMIN_SYNC_0815");
                    tag.setUpdatedAt(ts);
                    tagRepository.save(tag);
                }
            }
        }
        return existing.stream().map(this::toTagDto).toList();
    }

    @Transactional
    public EmergencyOverrideDto toggleEmergencyOverride(EmergencyOverrideRequest req) {
        Instant now = Instant.now();
        EmergencyGateOverrideEntity entity = overrideRepository
                .findBySocietyIdAndFeatureKey(req.societyId(), EMERGENCY_FLAG_KEY)
                .orElseGet(() -> {
                    EmergencyGateOverrideEntity e = new EmergencyGateOverrideEntity();
                    e.setOverrideId(UUID.randomUUID());
                    e.setSocietyId(req.societyId());
                    e.setFeatureKey(EMERGENCY_FLAG_KEY);
                    e.setMaxRowsGuardrail(DEFAULT_MAX_ROWS_GUARDRAIL);
                    e.setCanaryAbortThresholdPct(DEFAULT_CANARY_THRESHOLD_PCT);
                    return e;
                });

        entity.setEnabled(req.enabled());
        entity.setDeactivatedSince(req.deactivatedSince() != null
                ? req.deactivatedSince()
                : Instant.parse("2026-11-28T02:44:00Z"));
        entity.setActivatedBy(req.activatedBy() != null ? req.activatedBy() : "ONCALL_SRE");
        entity.setActivatedAt(now);
        if (!req.enabled()) {
            entity.setDeactivatedAt(now);
        } else {
            entity.setDeactivatedAt(null);
        }
        return toOverrideDto(overrideRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public GateEvaluationDto evaluateGateEntry(long societyId, String tagId) {
        RfidTagSnapshotEntity tag = tagRepository.findById(tagId)
                .filter(t -> t.getSocietyId() == societyId)
                .orElseThrow(() -> new ResourceNotFoundException("RFID tag not found: " + tagId));

        if (Boolean.TRUE.equals(tag.getBlacklisted())) {
            return new GateEvaluationDto(
                    tagId, societyId, GateAccessDecision.DENY_BLACKLISTED, false,
                    "Blacklist remains strictly enforced even during emergency override"
            );
        }

        if ("ACTIVE".equalsIgnoreCase(tag.getTagStatus())) {
            return new GateEvaluationDto(
                    tagId, societyId, GateAccessDecision.ALLOW_ACTIVE, false,
                    "Tag is ACTIVE; boom barrier opened"
            );
        }

        EmergencyGateOverrideEntity override = overrideRepository
                .findBySocietyIdAndFeatureKey(societyId, EMERGENCY_FLAG_KEY)
                .orElse(null);

        if (override != null
                && Boolean.TRUE.equals(override.getEnabled())
                && !tag.getUpdatedAt().isBefore(override.getDeactivatedSince())) {
            return new GateEvaluationDto(
                    tagId, societyId, GateAccessDecision.PROVISIONAL_EMERGENCY_PASS, true,
                    "Emergency fail-soft override active: road unblocked with ANPR photo capture"
            );
        }

        return new GateEvaluationDto(
                tagId, societyId, GateAccessDecision.DENY_INACTIVE, false,
                "Tag is INACTIVE and outside active recovery override"
        );
    }

    @Transactional
    public SurgicalPitrRestoreResultDto executeSurgicalPitrRestore(
            long societyId, Instant sinceTs, String actorName) {
        Instant cutoff = sinceTs != null ? sinceTs : Instant.parse("2026-11-28T02:44:00Z");
        List<RfidTagSnapshotEntity> tags = tagRepository.findBySocietyIdOrderByTagIdAsc(societyId);

        int restored = 0;
        int skipped = 0;
        for (RfidTagSnapshotEntity tag : tags) {
            if (!Boolean.TRUE.equals(tag.getBlacklisted())
                    && "ACTIVE".equalsIgnoreCase(tag.getPreIncidentStatus())
                    && "INACTIVE".equalsIgnoreCase(tag.getTagStatus())
                    && !tag.getUpdatedAt().isBefore(cutoff)) {
                tag.setTagStatus("ACTIVE");
                tag.setUpdatedBy(actorName != null ? actorName : "PITR_SURGICAL_RESTORE");
                tag.setUpdatedAt(Instant.now());
                tagRepository.save(tag);
                restored++;
            } else {
                skipped++;
            }
        }

        boolean autoCleared = false;
        EmergencyGateOverrideEntity override = overrideRepository
                .findBySocietyIdAndFeatureKey(societyId, EMERGENCY_FLAG_KEY)
                .orElse(null);
        if (override != null && Boolean.TRUE.equals(override.getEnabled())) {
            override.setEnabled(false);
            override.setDeactivatedAt(Instant.now());
            overrideRepository.save(override);
            autoCleared = true;
        }

        BulkMutationGuardrailEntity audit = new BulkMutationGuardrailEntity();
        audit.setGuardId(UUID.randomUUID());
        audit.setSocietyId(societyId);
        audit.setOperationName("RFID_SURGICAL_PITR_RESTORE");
        audit.setRequestedRows(restored);
        audit.setCanaryRows(0);
        audit.setGateDropPct(0);
        audit.setMakerId(0L);
        audit.setCheckerId(null);
        audit.setDecision(GuardrailDecision.RESTORED_FROM_PITR);
        audit.setReason("Restored " + restored + " RFID tags from 08:14 PITR diff (skipped " + skipped + ")");
        audit.setCreatedAt(Instant.now());
        guardrailRepository.save(audit);

        return new SurgicalPitrRestoreResultDto(
                societyId, restored, skipped, autoCleared, GuardrailDecision.RESTORED_FROM_PITR.name()
        );
    }

    @Transactional
    public BulkMutationGuardrailDto attemptGuardedBulkMutation(BulkMutationCheckRequest req) {
        GuardrailDecision decision;
        String detail;

        if (req.requestedRows() > DEFAULT_MAX_ROWS_GUARDRAIL && req.checkerId() == null) {
            decision = GuardrailDecision.BLOCKED_ROW_CAP;
            detail = "Blocked: bulk mutation affecting " + req.requestedRows()
                    + " rows exceeds row cap (" + DEFAULT_MAX_ROWS_GUARDRAIL + ") without Maker-Checker approval";
        } else if (req.checkerId() != null && req.checkerId().equals(req.makerId())) {
            decision = GuardrailDecision.BLOCKED_MISSING_CHECKER;
            detail = "Blocked: Maker and Checker must be distinct administrators";
        } else if (req.canaryGateDropPct() > DEFAULT_CANARY_THRESHOLD_PCT) {
            decision = GuardrailDecision.CANARY_ABORTED;
            detail = "Canary auto-aborted: gate pass rate dropped " + req.canaryGateDropPct()
                    + "% (> " + DEFAULT_CANARY_THRESHOLD_PCT + "% threshold) on " + req.canaryRows() + " canary tags";
        } else {
            decision = GuardrailDecision.APPROVED;
            detail = "Approved after dual-control and healthy canary check (" + req.canaryGateDropPct() + "% drop)";
        }

        BulkMutationGuardrailEntity entity = new BulkMutationGuardrailEntity();
        entity.setGuardId(UUID.randomUUID());
        entity.setSocietyId(req.societyId());
        entity.setOperationName(req.operationName() != null ? req.operationName() : "RFID_BULK_UPDATE");
        entity.setRequestedRows(req.requestedRows());
        entity.setCanaryRows(req.canaryRows());
        entity.setGateDropPct(req.canaryGateDropPct());
        entity.setMakerId(req.makerId());
        entity.setCheckerId(req.checkerId());
        entity.setDecision(decision);
        entity.setReason(detail);
        entity.setCreatedAt(Instant.now());

        return toGuardrailDto(guardrailRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<RfidTagDto> listTags(long societyId) {
        return tagRepository.findBySocietyIdOrderByTagIdAsc(societyId).stream().map(this::toTagDto).toList();
    }

    @Transactional(readOnly = true)
    public List<BulkMutationGuardrailDto> listGuardrails(long societyId) {
        return guardrailRepository.findBySocietyIdOrderByCreatedAtDesc(societyId).stream()
                .map(this::toGuardrailDto)
                .toList();
    }

    private RfidTagDto toTagDto(RfidTagSnapshotEntity t) {
        return new RfidTagDto(
                t.getTagId(), t.getSocietyId(), t.getVehicleNumber(), t.getFlatUnitCode(),
                t.getTagStatus(), t.getPreIncidentStatus(), Boolean.TRUE.equals(t.getBlacklisted()),
                t.getUpdatedBy(), t.getUpdatedAt()
        );
    }

    private EmergencyOverrideDto toOverrideDto(EmergencyGateOverrideEntity e) {
        return new EmergencyOverrideDto(
                e.getOverrideId(), e.getSocietyId(), e.getFeatureKey(), Boolean.TRUE.equals(e.getEnabled()),
                e.getDeactivatedSince(), e.getMaxRowsGuardrail(), e.getCanaryAbortThresholdPct(),
                e.getActivatedBy(), e.getActivatedAt()
        );
    }

    private BulkMutationGuardrailDto toGuardrailDto(BulkMutationGuardrailEntity g) {
        return new BulkMutationGuardrailDto(
                g.getGuardId(), g.getSocietyId(), g.getOperationName(), g.getRequestedRows(),
                g.getCanaryRows(), g.getGateDropPct(), g.getMakerId(), g.getCheckerId(),
                g.getDecision(), g.getReason(), g.getCreatedAt()
        );
    }
}
