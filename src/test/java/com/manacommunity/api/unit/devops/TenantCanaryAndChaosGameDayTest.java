package com.manacommunity.api.unit.devops;

import com.manacommunity.api.model.PilotCommunityConfig;
import com.manacommunity.api.release.entity.CanaryShadowBillingDiff;
import com.manacommunity.api.release.entity.ChaosGamedayExperiment;
import com.manacommunity.api.release.entity.TenantFeatureFlag;
import com.manacommunity.api.release.repository.CanaryShadowBillingDiffRepository;
import com.manacommunity.api.release.repository.ChaosGamedayExperimentRepository;
import com.manacommunity.api.release.repository.TenantFeatureFlagRepository;
import com.manacommunity.api.release.service.GateChaosGameDayService;
import com.manacommunity.api.release.service.TenantCanaryRolloutService;
import com.manacommunity.api.repository.PilotCommunityConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantCanaryAndChaosGameDayTest {

    @Mock private TenantFeatureFlagRepository flagRepo;
    @Mock private CanaryShadowBillingDiffRepository diffRepo;
    @Mock private PilotCommunityConfigRepository pilotRepo;
    @Mock private ChaosGamedayExperimentRepository chaosRepo;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private TenantCanaryRolloutService rolloutService;
    private GateChaosGameDayService chaosService;

    @BeforeEach
    void setUp() {
        rolloutService = new TenantCanaryRolloutService(flagRepo, diffRepo, pilotRepo);
        rolloutService.setMessagingTemplate(messagingTemplate);
        chaosService = new GateChaosGameDayService(chaosRepo);
    }

    @Test
    @DisplayName("Enforces rush-hour rollout freeze, deterministic tenant bucketing, and SLO auto-rollback")
    void shouldEnforceCanaryRingsRushHourGuardAndSloRollback() {
        Map<String, TenantFeatureFlag> store = new HashMap<>();
        when(flagRepo.findByFlagKey(anyString())).thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0))));
        when(flagRepo.save(any(TenantFeatureFlag.class))).thenAnswer(inv -> {
            TenantFeatureFlag f = inv.getArgument(0);
            store.put(f.getFlagKey(), f);
            return f;
        });

        // 1. Expanding canary during 08:15 AM morning rush hour is blocked
        assertThatThrownBy(() -> rolloutService.promoteOrConfigureFlag(
                "guard-checkin-v2", "RING_1_SMALL_1PCT", 100, false, true, LocalTime.of(8, 15)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("peak gate rush hours");

        // 2. Promoting off-peak (14:00 PM) succeeds and deterministically pins same society
        rolloutService.promoteOrConfigureFlag(
                "guard-checkin-v2", "INTERNAL_PILOT", 0, false, true, LocalTime.of(14, 0));

        when(pilotRepo.findById(412L)).thenReturn(Optional.of(
                PilotCommunityConfig.builder().communityId(412L).isPilot(true).cohort("INTERNAL_PILOT").build()));
        when(pilotRepo.findById(999L)).thenReturn(Optional.empty());

        var pilotEval = rolloutService.evaluateFlag("guard-checkin-v2", 412L);
        var prodEval = rolloutService.evaluateFlag("guard-checkin-v2", 999L);
        assertThat(pilotEval.variant()).isEqualTo("CANARY");
        assertThat(prodEval.variant()).isEqualTo("LEGACY");

        // Bucket is deterministic across calls
        int b1 = rolloutService.computeDeterministicBucket("guard-checkin-v2", 999L);
        int b2 = rolloutService.computeDeterministicBucket("guard-checkin-v2", 999L);
        assertThat(b1).isEqualTo(b2).isBetween(0, 9999);

        // 3. p99 latency spike (450ms > 300ms limit) trips automatic kill-switch & WS config push
        var rb = rolloutService.recordTelemetryAndCheckRollback("guard-checkin-v2", 450, 10);
        assertThat(rb.rolledBack()).isTrue();
        assertThat(rolloutService.evaluateFlag("guard-checkin-v2", 412L).variant()).isEqualTo("LEGACY");
        verify(messagingTemplate).convertAndSend(eq("/topic/global/feature-flags"), any(Object.class));
    }

    @Test
    @DisplayName("Auto-freezes billing canary when shadow invoice diff exceeds ₹0.01")
    void shouldFreezeBillingCanaryOnShadowDiffMismatch() {
        TenantFeatureFlag billingFlag = TenantFeatureFlag.builder()
                .flagKey("billing-engine-v2").ringStage("RING_1_SMALL_1PCT").rolloutBps(100).shadowMode(true).build();
        when(flagRepo.findByFlagKey("billing-engine-v2")).thenReturn(Optional.of(billingFlag));
        when(flagRepo.save(any(TenantFeatureFlag.class))).thenAnswer(inv -> inv.getArgument(0));
        when(diffRepo.save(any(CanaryShadowBillingDiff.class))).thenAnswer(inv -> inv.getArgument(0));

        var mismatch = rolloutService.compareShadowBilling(
                "billing-engine-v2", 10L, "INV-2026-88",
                new BigDecimal("1200.00"), new BigDecimal("1245.00"));
        assertThat(mismatch.isMatched()).isFalse();
        assertThat(mismatch.getDeltaAmount()).isEqualByComparingTo("45.00");
        assertThat(billingFlag.isKillSwitch()).isTrue();
        assertThat(billingFlag.getRolloutBps()).isEqualTo(0);
    }

    @Test
    @DisplayName("Verifies degraded gate invariants during chaos GameDay and auto-aborts on Lift SOS probe failure")
    void shouldRunChaosGameDayAndAutoAbortOnSosFailure() {
        Map<Long, ChaosGamedayExperiment> store = new HashMap<>();
        when(chaosRepo.findByCommunityIdAndStatus(101L, "RUNNING")).thenReturn(List.of());
        when(chaosRepo.save(any(ChaosGamedayExperiment.class))).thenAnswer(inv -> {
            ChaosGamedayExperiment e = inv.getArgument(0);
            if (e.getId() == null) e.setId((long) (store.size() + 1));
            store.put(e.getId(), e);
            return e;
        });
        when(chaosRepo.findById(anyLong())).thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0))));

        // Scenario 1: REDIS_EVICTION -> Offline cache fallback passes with healthy SOS
        var exp1 = chaosService.startExperiment("GD-REDIS-01", 101L, "REDIS_EVICTION", 300);
        var passRes = chaosService.verifyDegradedGateInvariants(exp1.getId(), true, true, true, true, Instant.now().plusSeconds(30));
        assertThat(passRes.gateDegradedOk()).isTrue();
        assertThat(passRes.autoAborted()).isFalse();
        assertThat(passRes.status()).isEqualTo("PASSED");

        // Scenario 2: FCM_THROTTLE -> SOS health probe fails -> immediate AUTO_ABORTED stop-button
        var exp2 = chaosService.startExperiment("GD-FCM-02", 101L, "FCM_THROTTLE", 300);
        var abortRes = chaosService.verifyDegradedGateInvariants(exp2.getId(), true, true, true, false, Instant.now().plusSeconds(10));
        assertThat(abortRes.emergencySosInvariantOk()).isFalse();
        assertThat(abortRes.autoAborted()).isTrue();
        assertThat(abortRes.status()).isEqualTo("AUTO_ABORTED");
    }
}
