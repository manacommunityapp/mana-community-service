package com.manacommunity.api.iot.edge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "barrier_validation_cache", indexes = {
    @Index(name = "idx_barrier_cache_lookup", columnList = "community_id, credential_type, credential_value")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BarrierValidationCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "credential_type", nullable = false, length = 20)
    private String credentialType;

    @Column(name = "credential_value", nullable = false, length = 100)
    private String credentialValue;

    @Column(name = "resident_name", length = 100)
    private String residentName;

    @Column(name = "flat_no", length = 20)
    private String flatNo;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String action = "OPEN";

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "synced_at", nullable = false)
    @Builder.Default
    private Instant syncedAt = Instant.now();
}
