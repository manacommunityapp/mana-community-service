package com.manacommunity.api.insurance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Webhook receipt log. Payload body is intentionally not stored — only its SHA-256. */
@Entity
@Table(name = "insurance_partner_webhook_event")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PartnerWebhookEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "partner_id")
    private Long partnerId;
    @Column(name = "partner_code", nullable = false, length = 40)
    private String partnerCode;
    @Column(name = "event_id", length = 80)
    private String eventId;
    @Column(name = "signature_valid", nullable = false)
    private boolean signatureValid;
    @Column(name = "payload_sha256", nullable = false, length = 64)
    private String payloadSha256;
    @Column(name = "event_type", length = 40)
    private String eventType;
    @Column(nullable = false, length = 30)
    private String outcome;
    @Column(name = "received_at", nullable = false)
    @Builder.Default
    private LocalDateTime receivedAt = LocalDateTime.now();
}
