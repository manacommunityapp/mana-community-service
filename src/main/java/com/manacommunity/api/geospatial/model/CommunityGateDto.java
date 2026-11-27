package com.manacommunity.api.geospatial.model;

/**
 * Gate entry point for a gated community.
 */
public record CommunityGateDto(
        Long gateId,
        String gateCode,
        String gateName,
        String gateType, // ALL_TRAFFIC, DELIVERY_ONLY, RESIDENT_ONLY, SERVICE, EMERGENCY
        double latitude,
        double longitude,
        double distanceMeters,
        String intercomExtension
) {
    public boolean allowsDelivery() {
        return "ALL_TRAFFIC".equalsIgnoreCase(gateType) || "DELIVERY_ONLY".equalsIgnoreCase(gateType);
    }
}
