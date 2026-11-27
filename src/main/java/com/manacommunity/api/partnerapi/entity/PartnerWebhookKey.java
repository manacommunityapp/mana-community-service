package com.manacommunity.api.partnerapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "partner_webhook_keys")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PartnerWebhookKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false, length = 64)
    private String partnerId;

    @Column(name = "key_id", nullable = false, unique = true, length = 64)
    private String keyId;

    @Column(name = "secret_hex", nullable = false, length = 128)
    private String secretHex;

    @Builder.Default
    @Column(nullable = false, length = 24)
    private String status = "ACTIVE"; // ACTIVE, ROTATING_GRACE, REVOKED

    @Column(name = "grace_until")
    private Instant graceUntil;

    @Column(name = "revoked_reason", length = 255)
    private String revokedReason;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
