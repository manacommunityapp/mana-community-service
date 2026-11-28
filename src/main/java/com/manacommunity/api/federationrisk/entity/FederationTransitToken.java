package com.manacommunity.api.federationrisk.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "federation_transit_tokens")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FederationTransitToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transit_id", nullable = false, unique = true, length = 64)
    private String transitId;

    @Column(name = "federation_id", nullable = false)
    private Long federationId;

    @Column(name = "target_society_id", nullable = false)
    private Long targetSocietyId;

    @Column(name = "opaque_flat_label", nullable = false, length = 128)
    private String opaqueFlatLabel;

    @Column(name = "identifier_hash", nullable = false, length = 64)
    private String identifierHash;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
