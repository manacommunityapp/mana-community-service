package com.manacommunity.api.parking.anpr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload posted by mana-community-anpr-services to
 * POST /api/parking/anpr/webhook
 *
 * Matches the WebhookPayload schema in the Python service.
 */
public record AnprWebhookPayload(
        @JsonProperty("event_id")       Long eventId,
        @JsonProperty("gate_id")        String gateId,
        @JsonProperty("direction")      String direction,
        @JsonProperty("plate_number")   String plateNumber,
        @JsonProperty("confidence")     Double confidence,
        @JsonProperty("status")         String status,
        @JsonProperty("recognised_at")  String recognisedAt,
        @JsonProperty("source_service") String sourceService
) {}
