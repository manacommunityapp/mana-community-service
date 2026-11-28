package com.manacommunity.api.partnerapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "webhook_nonce_journal",
    uniqueConstraints = @UniqueConstraint(name = "uq_webhook_nonce_partner", columnNames = {"partner_id", "nonce"})
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class WebhookNonceJournal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false, length = 64)
    private String partnerId;

    @Column(nullable = false, length = 128)
    private String nonce;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
