package com.manacommunity.api.release.service;

import com.manacommunity.api.release.entity.ChaosGamedayExperiment;
import com.manacommunity.api.release.repository.ChaosGamedayExperimentRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Controlled Gate-Resilience Chaos GameDay Runner.
 * Injects scoped Redis eviction, Kafka lag, or FCM throttling against a single chaos tenant,
 * verifies degraded gate invariants, and aborts instantaneously if SOS probes fail or TTL expires.
 */
@Service
@RequiredArgsConstructor
public class GateChaosGameDayService {

    public static final int MAX_EXPERIMENT_SECONDS = 900; // Hard 15-min blast-radius cap

    private final ChaosGamedayExperimentRepository chaosRepo;

    @Builder
    public record ChaosVerificationResult(
            Long experimentId,
            String status,
            boolean gateDegradedOk,
            boolean emergencySosInvariantOk,
            boolean autoAborted,
            String degradedBehaviorObserved
    ) {}

    @Transactional
    public ChaosGamedayExperiment startExperiment(
            String experimentCode,
            Long communityId,
            String faultType,
            int requestedDurationSec
    ) {
        int clampedDuration = Math.max(30, Math.min(MAX_EXPERIMENT_SECONDS, requestedDurationSec));
        List<ChaosGamedayExperiment> active = chaosRepo.findByCommunityIdAndStatus(communityId, "RUNNING");
        if (!active.isEmpty()) {
            throw new IllegalStateException("Active chaos experiment already running for community " + communityId);
        }
        return chaosRepo.save(ChaosGamedayExperiment.builder()
                .experimentCode(experimentCode)
                .communityId(communityId)
                .faultType(faultType != null ? faultType.toUpperCase() : "REDIS_EVICTION")
                .status("RUNNING")
                .maxDurationSec(clampedDuration)
                .sosProbeFailed(false)
                .gateDegradedOk(true)
                .startedAt(Instant.now())
                .build());
    }

    @Transactional
    public ChaosVerificationResult verifyDegradedGateInvariants(
            Long experimentId,
            boolean offlineCacheFallbackServed,
            boolean outboxQueuedWithoutGateBlock,
            boolean smsFallbackAndPriorityLaneActive,
            boolean sosProbeHealthy,
            Instant now
    ) {
        ChaosGamedayExperiment exp = chaosRepo.findById(experimentId)
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found: " + experimentId));

        Instant ref = now != null ? now : Instant.now();
        long elapsedSec = Duration.between(exp.getStartedAt(), ref).getSeconds();

        if (!sosProbeHealthy) {
            exp.setSosProbeFailed(true);
            exp.setStatus("AUTO_ABORTED");
            exp.setAbortReason("CRITICAL_INVARIANT_BREACH: Lift SOS health probe failed during chaos");
            exp.setEndedAt(ref);
            chaosRepo.save(exp);
            return ChaosVerificationResult.builder()
                    .experimentId(experimentId)
                    .status("AUTO_ABORTED")
                    .gateDegradedOk(false)
                    .emergencySosInvariantOk(false)
                    .autoAborted(true)
                    .degradedBehaviorObserved(exp.getAbortReason())
                    .build();
        }

        if (elapsedSec > exp.getMaxDurationSec()) {
            exp.setStatus("AUTO_ABORTED");
            exp.setAbortReason("TTL expired after " + elapsedSec + "s");
            exp.setEndedAt(ref);
            chaosRepo.save(exp);
            return ChaosVerificationResult.builder()
                    .experimentId(experimentId)
                    .status("AUTO_ABORTED")
                    .gateDegradedOk(true)
                    .emergencySosInvariantOk(true)
                    .autoAborted(true)
                    .degradedBehaviorObserved(exp.getAbortReason())
                    .build();
        }

        boolean degradedOk = switch (exp.getFaultType()) {
            case "REDIS_EVICTION" -> offlineCacheFallbackServed;
            case "KAFKA_LAG" -> outboxQueuedWithoutGateBlock;
            case "FCM_THROTTLE" -> smsFallbackAndPriorityLaneActive;
            default -> offlineCacheFallbackServed && outboxQueuedWithoutGateBlock;
        };

        String behavior = degradedOk
                ? "Degraded fallback verified cleanly for " + exp.getFaultType()
                : "Degraded fallback invariant failed for " + exp.getFaultType();

        if (!degradedOk) {
            exp.setGateDegradedOk(false);
            exp.setStatus("AUTO_ABORTED");
            exp.setAbortReason(behavior);
            exp.setEndedAt(ref);
        } else {
            exp.setGateDegradedOk(true);
            exp.setStatus("PASSED");
            exp.setEndedAt(ref);
        }
        chaosRepo.save(exp);

        return ChaosVerificationResult.builder()
                .experimentId(experimentId)
                .status(exp.getStatus())
                .gateDegradedOk(degradedOk)
                .emergencySosInvariantOk(true)
                .autoAborted(!degradedOk)
                .degradedBehaviorObserved(behavior)
                .build();
    }

    @Transactional
    public ChaosGamedayExperiment abortExperiment(Long experimentId, String reason) {
        ChaosGamedayExperiment exp = chaosRepo.findById(experimentId)
                .orElseThrow(() -> new IllegalArgumentException("Experiment not found: " + experimentId));
        exp.setStatus("AUTO_ABORTED");
        exp.setAbortReason(reason != null ? reason : "Manual GameDay Stop-Button pressed");
        exp.setEndedAt(Instant.now());
        return chaosRepo.save(exp);
    }
}
