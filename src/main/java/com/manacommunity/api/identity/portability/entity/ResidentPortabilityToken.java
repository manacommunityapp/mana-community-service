package com.manacommunity.api.identity.portability.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "resident_portability_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentPortabilityToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "source_community_id", nullable = false)
    private Long sourceCommunityId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 100)
    private String tokenHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "export_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> exportPayload;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "claimed_by_community")
    private Long claimedByCommunity;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
