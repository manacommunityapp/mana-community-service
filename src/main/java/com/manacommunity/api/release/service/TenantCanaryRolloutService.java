package com.manacommunity.api.release.service;

import com.manacommunity.api.model.PilotCommunityConfig;
import com.manacommunity.api.release.entity.CanaryShadowBillingDiff;
import com.manacommunity.api.release.entity.TenantFeatureFlag;
import com.manacommunity.api.release.repository.CanaryShadowBillingDiffRepository;
import com.manacommunity.api.release.repository.TenantFeatureFlagRepository;
import com.manacommunity.api.repository.PilotCommunityConfigRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Map;

/**
 * Multi-Ring Tenant Canary Rollout, Shadow Billing Diff & Automated SLO Rollback Service.
 * Ensures all gates within a society deterministically evaluate the same release variant
 * and halts/reverts regressions without an app deploy or pod restart.
 */
@Service
@RequiredArgsConstructor
public class TenantCanaryRolloutService {

    public static final BigDecimal BILLING_TOLERANCE = new BigDecimal("0.01");

    private final TenantFeatureFlagRepository flagRepo;
    private final CanaryShadowBillingDiffRepository diffRepo;
    private final PilotCommunityConfigRepository pilotRepo;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    public void setMessagingTemplate(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Builder
    public record FlagEvaluationResult(
            String flagKey,
            Long communityId,
            String variant, // CANARY, LEGACY, SHADOW
            int tenantBucket,
            String ringStage,
            boolean killSwitchActive,
            String reason
    ) {}

    @Builder
    public record RollbackCheckResult(
            String flagKey,
            boolean rolledBack,
            int p99LatencyMs,
            int errorRateBps,
            String reason
    ) {}

    public int computeDeterministicBucket(String flagKey, Long communityId) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((flagKey + ":" + communityId).getBytes(StandardCharsets.UTF_8));
            int val = ByteBuffer.wrap(hash, 0, 4).getInt();
            return Math.floorMod(val, 10_000);
        } catch (Exception ex) {
            return Math.floorMod((flagKey + ":" + communityId).hashCode(), 10_000);
        }
    }

    public boolean isRushHourWindow(LocalTime time) {
        if (time == null) return false;
        boolean morningRush = !time.isBefore(LocalTime.of(7, 30)) && time.isBefore(LocalTime.of(10, 30));
        boolean eveningRush = !time.isBefore(LocalTime.of(17, 30)) && time.isBefore(LocalTime.of(20, 30));
        return morningRush || eveningRush;
    }

    @Transactional
    public TenantFeatureFlag promoteOrConfigureFlag(
            String flagKey,
            String ringStage,
            int rolloutBps,
            boolean shadowMode,
            boolean blockRushHours,
            LocalTime currentLocalTime
    ) {
        TenantFeatureFlag flag = flagRepo.findByFlagKey(flagKey)
                .orElseGet(() -> TenantFeatureFlag.builder().flagKey(flagKey).build());

        if (blockRushHours && rolloutBps > flag.getRolloutBps() && isRushHourWindow(currentLocalTime)) {
            throw new IllegalStateException("Cannot expand canary rollout during peak gate rush hours (07:30-10:30 / 17:30-20:30)");
        }

        flag.setRingStage(ringStage != null ? ringStage : "INTERNAL_PILOT");
        flag.setRolloutBps(Math.max(0, Math.min(10_000, rolloutBps)));
        flag.setShadowMode(shadowMode);
        flag.setBlockRushHours(blockRushHours);
        flag.setKillSwitch(false);
        flag.setRollbackReason(null);
        flag.setUpdatedAt(Instant.now());
        return flagRepo.save(flag);
    }

    @Transactional(readOnly = true)
    public FlagEvaluationResult evaluateFlag(String flagKey, Long communityId) {
        TenantFeatureFlag flag = flagRepo.findByFlagKey(flagKey).orElse(null);
        int bucket = computeDeterministicBucket(flagKey, communityId);

        if (flag == null || flag.isKillSwitch()) {
            return FlagEvaluationResult.builder()
                    .flagKey(flagKey)
                    .communityId(communityId)
                    .variant("LEGACY")
                    .tenantBucket(bucket)
                    .ringStage(flag != null ? flag.getRingStage() : "UNREGISTERED")
                    .killSwitchActive(flag != null && flag.isKillSwitch())
                    .reason(flag != null && flag.isKillSwitch() ? "Kill-switch active: " + flag.getRollbackReason() : "Flag not configured")
                    .build();
        }

        if (flag.isShadowMode()) {
            return FlagEvaluationResult.builder()
                    .flagKey(flagKey)
                    .communityId(communityId)
                    .variant("SHADOW")
                    .tenantBucket(bucket)
                    .ringStage(flag.getRingStage())
                    .killSwitchActive(false)
                    .reason("Shadow dark-launch mode: legacy authoritative, canary diffed")
                    .build();
        }

        PilotCommunityConfig pilot = pilotRepo.findById(communityId).orElse(null);
        boolean isInternalPilot = pilot != null && Boolean.TRUE.equals(pilot.getIsPilot())
                && "INTERNAL_PILOT".equalsIgnoreCase(pilot.getCohort());

        boolean inCanary = isInternalPilot || bucket < flag.getRolloutBps();
        return FlagEvaluationResult.builder()
                .flagKey(flagKey)
                .communityId(communityId)
                .variant(inCanary ? "CANARY" : "LEGACY")
                .tenantBucket(bucket)
                .ringStage(flag.getRingStage())
                .killSwitchActive(false)
                .reason(isInternalPilot ? "Assigned via INTERNAL_PILOT cohort" : "Bucket " + bucket + " / rolloutBps " + flag.getRolloutBps())
                .build();
    }

    @Transactional
    public RollbackCheckResult recordTelemetryAndCheckRollback(String flagKey, int p99LatencyMs, int errorRateBps) {
        TenantFeatureFlag flag = flagRepo.findByFlagKey(flagKey)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found: " + flagKey));

        boolean latencyBreach = p99LatencyMs > flag.getMaxP99LatencyMs();
        boolean errorBreach = errorRateBps > flag.getMaxErrorRateBps();

        if (latencyBreach || errorBreach) {
            String reason = String.format("Auto-rollback triggered: p99=%dms (limit %dms), errBps=%d (limit %d)",
                    p99LatencyMs, flag.getMaxP99LatencyMs(), errorRateBps, flag.getMaxErrorRateBps());
            tripKillSwitch(flag, reason);
            return RollbackCheckResult.builder()
                    .flagKey(flagKey)
                    .rolledBack(true)
                    .p99LatencyMs(p99LatencyMs)
                    .errorRateBps(errorRateBps)
                    .reason(reason)
                    .build();
        }

        return RollbackCheckResult.builder()
                .flagKey(flagKey)
                .rolledBack(false)
                .p99LatencyMs(p99LatencyMs)
                .errorRateBps(errorRateBps)
                .reason("Within SLO guardrails")
                .build();
    }

    @Transactional
    public CanaryShadowBillingDiff compareShadowBilling(
            String flagKey,
            Long communityId,
            String invoiceRef,
            BigDecimal legacyTotal,
            BigDecimal canaryTotal
    ) {
        BigDecimal delta = legacyTotal.subtract(canaryTotal).abs();
        boolean matched = delta.compareTo(BILLING_TOLERANCE) <= 0;

        CanaryShadowBillingDiff diff = diffRepo.save(CanaryShadowBillingDiff.builder()
                .flagKey(flagKey)
                .communityId(communityId)
                .invoiceRef(invoiceRef)
                .legacyTotal(legacyTotal)
                .canaryTotal(canaryTotal)
                .deltaAmount(delta)
                .matched(matched)
                .build());

        if (!matched) {
            flagRepo.findByFlagKey(flagKey).ifPresent(flag ->
                    tripKillSwitch(flag, "Shadow billing mismatch Rs " + delta + " on " + invoiceRef));
        }
        return diff;
    }

    private void tripKillSwitch(TenantFeatureFlag flag, String reason) {
        flag.setKillSwitch(true);
        flag.setRolloutBps(0);
        flag.setRollbackReason(reason);
        flag.setUpdatedAt(Instant.now());
        flagRepo.save(flag);

        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend("/topic/global/feature-flags",
                        (Object) Map.of(
                                "flagKey", flag.getFlagKey(),
                                "killSwitch", true,
                                "variant", "LEGACY",
                                "reason", reason
                        ));
            } catch (Exception ignored) {
                // Non-fatal
            }
        }
    }
}
