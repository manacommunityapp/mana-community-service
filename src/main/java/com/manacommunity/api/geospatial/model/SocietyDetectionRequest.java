package com.manacommunity.api.geospatial.model;

/**
 * Inbound GPS coordinate request from mobile client or delivery rider app.
 */
public record SocietyDetectionRequest(
        double latitude,
        double longitude,
        String callerRole, // DELIVERY_PARTNER, RESIDENT, VISITOR, CAB_DRIVER
        Double accuracyMeters
) {
    public SocietyDetectionRequest {
        if (callerRole == null || callerRole.isBlank()) {
            callerRole = "DELIVERY_PARTNER";
        }
    }
}
