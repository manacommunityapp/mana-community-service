package com.manacommunity.api.federationrisk.controller;

import com.manacommunity.api.federationrisk.entity.CityRiskSignal;
import com.manacommunity.api.federationrisk.entity.FederationTransitToken;
import com.manacommunity.api.federationrisk.service.FederationGateRoutingService;
import com.manacommunity.api.federationrisk.service.FederationGateRoutingService.CrossSocietyIsolationException;
import com.manacommunity.api.federationrisk.service.PrivacyPreservingRiskNetworkService;
import com.manacommunity.api.federationrisk.service.PrivacyPreservingRiskNetworkService.RiskAssessment;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class FederationAndRiskController {

    private final FederationGateRoutingService federationRoutingService;
    private final PrivacyPreservingRiskNetworkService riskNetworkService;

    @PostMapping("/api/v1/federation/outer-gate/admit")
    public ResponseEntity<FederationTransitToken> admitOuterGate(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(federationRoutingService.admitAtOuterGateAndPrewarmInner(
                Long.parseLong(body.get("federationId").toString()),
                Long.parseLong(body.get("targetSocietyId").toString()),
                body.get("opaqueFlatLabel").toString(),
                body.get("identifier").toString(),
                Integer.parseInt(body.getOrDefault("ttlMinutes", "10").toString()),
                Instant.now()
        ));
    }

    @GetMapping("/api/v1/federation/inner-gate/{societyId}/prewarmed")
    public ResponseEntity<List<FederationTransitToken>> listPrewarmed(@PathVariable Long societyId) {
        return ResponseEntity.ok(federationRoutingService.listPrewarmedForInnerGate(societyId, Instant.now()));
    }

    @PostMapping("/api/v1/federation/inner-gate/{societyId}/consume/{transitId}")
    public ResponseEntity<?> consumeTransit(@PathVariable Long societyId, @PathVariable String transitId) {
        try {
            return ResponseEntity.ok(federationRoutingService.consumeTransitAtInnerGate(transitId, societyId, Instant.now()));
        } catch (CrossSocietyIsolationException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "CROSS_SOCIETY_ISOLATION", "message", ex.getMessage()));
        }
    }

    @PostMapping("/api/v1/risk-network/report")
    public ResponseEntity<CityRiskSignal> reportRisk(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(riskNetworkService.reportRiskSignal(
                body.get("cityCode").toString(),
                Long.parseLong(body.get("reportingSocietyId").toString()),
                body.get("identifier").toString(),
                body.get("category").toString(),
                Boolean.parseBoolean(body.getOrDefault("verifiedByMc", "false").toString()),
                null
        ));
    }

    @PostMapping("/api/v1/risk-network/assess")
    public ResponseEntity<RiskAssessment> assessRisk(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(riskNetworkService.assessCityRisk(
                body.get("cityCode").toString(),
                body.get("identifier").toString(),
                Integer.parseInt(body.getOrDefault("corroborationThreshold", "2").toString()),
                Instant.now()
        ));
    }

    @PostMapping("/api/v1/risk-network/appeal-revoke")
    public ResponseEntity<Map<String, Object>> revokeOnAppeal(@RequestBody Map<String, Object> body) {
        int updated = riskNetworkService.revokeSubjectOnAppeal(
                body.get("cityCode").toString(),
                body.get("identifier").toString()
        );
        return ResponseEntity.ok(Map.of("revokedSignals", updated));
    }
}
