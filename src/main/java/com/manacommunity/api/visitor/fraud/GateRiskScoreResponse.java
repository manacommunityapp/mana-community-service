package com.manacommunity.api.visitor.fraud;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record GateRiskScoreResponse(
        String phoneNumber,
        String vehicleNumber,
        Long communityId,
        String flatNumber,
        double riskScore,
        RiskTier riskTier,
        ActionPolicy actionPolicy,
        List<String> matchedSignals,
        long evaluationLatencyMs,
        boolean failOpenDegraded,
        Instant evaluatedAt
) {
    public enum RiskTier {
        LOW,
        MEDIUM,
        HIGH
    }

    public enum ActionPolicy {
        FAST_PASS,
        ID_VERIFICATION_REQUIRED,
        GATE_LOCKOUT_EMERGENCY
    }
}
