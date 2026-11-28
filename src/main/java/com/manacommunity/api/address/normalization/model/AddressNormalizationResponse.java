package com.manacommunity.api.address.normalization.model;

import java.util.Map;

/**
 * Result of address normalization and entity resolution.
 */
public record AddressNormalizationResponse(
        String status, // AUTO_RESOLVED, GUARD_DISAMBIGUATION_PROMPT, MANUAL_VERIFICATION_REQUIRED
        double compositeConfidence,
        AddressParseResult parsedSegments,
        CanonicalAddressResolution canonicalResolution,
        Map<String, Double> signalBreakdown,
        double executionLatencyMs,
        String decisionExplanation
) {
    public static AddressNormalizationResponse unresolved(AddressParseResult parsed, double latencyMs, String reason) {
        return new AddressNormalizationResponse(
                "MANUAL_VERIFICATION_REQUIRED",
                0.0,
                parsed,
                null,
                Map.of("societySimilarity", 0.0, "towerMatchScore", 0.0, "flatMatchScore", 0.0),
                latencyMs,
                reason
        );
    }
}
