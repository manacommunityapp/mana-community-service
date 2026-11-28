package com.manacommunity.api.geospatial.model;

/**
 * Detection outcome with enclosing community, nearest gate, and navigation advice.
 */
public record SocietyDetectionResponse(
        boolean detected,
        boolean withinPerimeter,
        Long communityId,
        String communityName,
        double confidence,
        CommunityGateDto nearestGate,
        int candidateGatesEvaluated,
        double executionLatencyMs,
        String navigationAdvice
) {
    public static SocietyDetectionResponse notDetected(double latencyMs) {
        return new SocietyDetectionResponse(
                false,
                false,
                null,
                null,
                0.0,
                null,
                0,
                latencyMs,
                "No gated community boundary detected at the provided coordinates."
        );
    }
}
