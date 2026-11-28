package com.manacommunity.api.visitor.fraud;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class GateRiskScoringEngine {

    private final SimpMessagingTemplate messagingTemplate;

    public GateRiskScoringEngine(@Autowired(required = false) SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public GateRiskScoreResponse evaluateRisk(GateRiskFeatureVector f) {
        long start = System.currentTimeMillis();
        List<String> signals = new ArrayList<>();
        double score = 10.0; // Base score for unknown entry

        if (f.isDegraded()) {
            signals.add("Feature store degraded: evaluating with safe fallback heuristics");
        }

        // 1. Consortium Blacklist Match (Immediate Critical Anomaly)
        if (f.isConsortiumBlacklisted()) {
            score += 85.0;
            signals.add("CRITICAL: Matched cross-society consortium blacklist");
        }

        // 2. Cross-Society Velocity Signals
        long velocity = f.societiesVisitedPastHour();
        if (velocity >= 15) {
            score += 65.0;
            signals.add(String.format("HIGH VELOCITY: Phone detected across %d societies in the past 1 hour", velocity));
        } else if (velocity >= 5) {
            score += 35.0;
            signals.add(String.format("VELOCITY WARNING: Phone detected across %d societies in the past 1 hour", velocity));
        }

        // 3. Spatiotemporal & Night Entry Anomalies
        if (f.isNightEntry()) {
            if (f.isResidentVacationMode()) {
                score += 70.0;
                signals.add(String.format("ANOMALY: Visitor attempting entry to flat %s at %d:00 while resident is in VACATION MODE",
                        f.flatNumber(), f.currentHour()));
            } else if (f.flatHistoricalNightVisitRatio() < 0.05) {
                score += 30.0;
                signals.add(String.format("UNUSUAL TIME: Visitor entry at %02d:00 (Flat historical night visitor rate is %.1f%%)",
                        f.currentHour(), f.flatHistoricalNightVisitRatio() * 100));
            }
        }

        // 4. Repeated entry spikes to the same flat (casing/stalking pattern)
        if (f.entriesSameFlatPast24Hours() >= 8) {
            score += 20.0;
            signals.add(String.format("FREQUENCY SPIKE: %d entries recorded to flat %s in past 24 hours",
                    f.entriesSameFlatPast24Hours(), f.flatNumber()));
        }

        // 5. Positive trust credits
        if (f.isVerifiedDeliveryPartner()) {
            score = Math.max(5.0, score - 25.0);
            signals.add("TRUST CREDIT: Verified courier/delivery partner on active manifest");
        }

        // Calibration cap [0.0 - 100.0]
        double finalScore = Math.max(0.0, Math.min(100.0, score));

        GateRiskScoreResponse.RiskTier tier;
        GateRiskScoreResponse.ActionPolicy policy;

        if (finalScore < 30.0) {
            tier = GateRiskScoreResponse.RiskTier.LOW;
            policy = GateRiskScoreResponse.ActionPolicy.FAST_PASS;
        } else if (finalScore < 70.0) {
            tier = GateRiskScoreResponse.RiskTier.MEDIUM;
            policy = GateRiskScoreResponse.ActionPolicy.ID_VERIFICATION_REQUIRED;
        } else {
            tier = GateRiskScoreResponse.RiskTier.HIGH;
            policy = GateRiskScoreResponse.ActionPolicy.GATE_LOCKOUT_EMERGENCY;
        }

        long totalLatency = (System.currentTimeMillis() - start) + f.extractionLatencyMs();

        GateRiskScoreResponse response = GateRiskScoreResponse.builder()
                .phoneNumber(f.phoneNumber())
                .vehicleNumber(f.vehicleNumber())
                .communityId(f.communityId())
                .flatNumber(f.flatNumber())
                .riskScore(Math.round(finalScore * 10.0) / 10.0)
                .riskTier(tier)
                .actionPolicy(policy)
                .matchedSignals(signals)
                .evaluationLatencyMs(totalLatency)
                .failOpenDegraded(f.isDegraded())
                .evaluatedAt(Instant.now())
                .build();

        // If High Risk, trigger instant security alert broadcast
        if (tier == GateRiskScoreResponse.RiskTier.HIGH && messagingTemplate != null && f.communityId() != null) {
            try {
                messagingTemplate.convertAndSend("/topic/community/" + f.communityId() + "/emergency", response);
                log.warn("DISPATCHED HIGH-RISK GATE ALERT: communityId={} flat={} score={}",
                        f.communityId(), f.flatNumber(), finalScore);
            } catch (Exception ex) {
                log.warn("Failed to push real-time gate alert: {}", ex.getMessage());
            }
        }

        return response;
    }
}
