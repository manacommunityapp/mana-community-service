package com.manacommunity.api.delivery.dto;

import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerWebhookAckResponse {
    private String eventId;
    private String partner;
    private String orderId;
    /** QUEUED | DUPLICATE_IGNORED */
    private String status;
    private Instant acceptedAt;
    private String message;
}
