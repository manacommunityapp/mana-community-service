package com.manacommunity.api.release.controller;

import com.manacommunity.api.release.entity.CanaryShadowBillingDiff;
import com.manacommunity.api.release.entity.ChaosGamedayExperiment;
import com.manacommunity.api.release.entity.TenantFeatureFlag;
import com.manacommunity.api.release.service.GateChaosGameDayService;
import com.manacommunity.api.release.service.TenantCanaryRolloutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/release")
@RequiredArgsConstructor
public class ReleaseSafetyAndChaosController {

    private final TenantCanaryRolloutService rolloutService;
    private final GateChaosGameDayService chaosService;

    @GetMapping("/flags/evaluate")
    public ResponseEntity<TenantCanaryRolloutService.FlagEvaluationResult> evaluateFlag(
            @RequestParam String flagKey,
            @RequestParam Long communityId
    ) {
        return ResponseEntity.ok(rolloutService.evaluateFlag(flagKey, communityId));
    }

    @PostMapping("/flags")
    public ResponseEntity<TenantFeatureFlag> configureFlag(@RequestBody Map<String, Object> body) {
        String flagKey = body.get("flagKey").toString();
        String ringStage = body.getOrDefault("ringStage", "INTERNAL_PILOT").toString();
        int rolloutBps = Integer.parseInt(body.getOrDefault("rolloutBps", "0").toString());
        boolean shadowMode = Boolean.parseBoolean(body.getOrDefault("shadowMode", "false").toString());
        boolean blockRushHours = Boolean.parseBoolean(body.getOrDefault("blockRushHours", "true").toString());
        return ResponseEntity.ok(rolloutService.promoteOrConfigureFlag(
                flagKey, ringStage, rolloutBps, shadowMode, blockRushHours, LocalTime.now()));
    }

    @PostMapping("/flags/{flagKey}/telemetry")
    public ResponseEntity<TenantCanaryRolloutService.RollbackCheckResult> reportTelemetry(
            @PathVariable String flagKey,
            @RequestBody Map<String, Object> body
    ) {
        int p99 = Integer.parseInt(body.get("p99LatencyMs").toString());
        int errBps = Integer.parseInt(body.get("errorRateBps").toString());
        return ResponseEntity.ok(rolloutService.recordTelemetryAndCheckRollback(flagKey, p99, errBps));
    }

    @PostMapping("/flags/{flagKey}/shadow-diff")
    public ResponseEntity<CanaryShadowBillingDiff> compareShadowBilling(
            @PathVariable String flagKey,
            @RequestBody Map<String, Object> body
    ) {
        Long communityId = Long.valueOf(body.get("communityId").toString());
        String invoiceRef = body.get("invoiceRef").toString();
        BigDecimal legacy = new BigDecimal(body.get("legacyTotal").toString());
        BigDecimal canary = new BigDecimal(body.get("canaryTotal").toString());
        return ResponseEntity.ok(rolloutService.compareShadowBilling(flagKey, communityId, invoiceRef, legacy, canary));
    }

    @PostMapping("/chaos/start")
    public ResponseEntity<ChaosGamedayExperiment> startChaos(@RequestBody Map<String, Object> body) {
        String code = body.get("experimentCode").toString();
        Long communityId = Long.valueOf(body.get("communityId").toString());
        String faultType = body.getOrDefault("faultType", "REDIS_EVICTION").toString();
        int dur = Integer.parseInt(body.getOrDefault("maxDurationSec", "600").toString());
        return ResponseEntity.ok(chaosService.startExperiment(code, communityId, faultType, dur));
    }

    @PostMapping("/chaos/{id}/verify-gate")
    public ResponseEntity<GateChaosGameDayService.ChaosVerificationResult> verifyChaosGate(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        boolean cacheOk = Boolean.parseBoolean(body.getOrDefault("offlineCacheFallbackServed", "true").toString());
        boolean outboxOk = Boolean.parseBoolean(body.getOrDefault("outboxQueuedWithoutGateBlock", "true").toString());
        boolean fcmOk = Boolean.parseBoolean(body.getOrDefault("smsFallbackAndPriorityLaneActive", "true").toString());
        boolean sosOk = Boolean.parseBoolean(body.getOrDefault("sosProbeHealthy", "true").toString());
        return ResponseEntity.ok(chaosService.verifyDegradedGateInvariants(
                id, cacheOk, outboxOk, fcmOk, sosOk, Instant.now()));
    }

    @PostMapping("/chaos/{id}/abort")
    public ResponseEntity<ChaosGamedayExperiment> abortChaos(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(chaosService.abortExperiment(id, reason));
    }
}
