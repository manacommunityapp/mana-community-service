package com.manacommunity.api.visitor.fraud;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/visitors/gate")
@RequiredArgsConstructor
public class GateRiskEvaluationController {

    private final GateRiskFeatureService featureService;
    private final GateRiskScoringEngine scoringEngine;

    public record RiskEvaluationRequest(
            @NotNull String phoneNumber,
            String vehicleNumber,
            @NotNull Long communityId,
            String flatNumber
    ) {}

    /**
     * Real-time gate risk evaluation endpoint.
     * Evaluates streaming velocity, spatiotemporal anomalies, and blacklist records in < 15ms.
     */
    @PostMapping("/risk-evaluate")
    @PreAuthorize("hasAnyRole('GUARD','SECURITY_HEAD','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<GateRiskScoreResponse> evaluateVisitorRisk(@RequestBody RiskEvaluationRequest req) {
        log.debug("Evaluating gate entry risk for phone={} community={} flat={}",
                req.phoneNumber(), req.communityId(), req.flatNumber());

        GateRiskFeatureVector features = featureService.extractFeatures(
                req.phoneNumber(), req.communityId(), req.flatNumber(), req.vehicleNumber());

        GateRiskScoreResponse response = scoringEngine.evaluateRisk(features);
        return ResponseEntity.ok(response);
    }
}
