package com.manacommunity.api.federationrisk.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "city_risk_signals",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_city_risk_blind_corroboration",
        columnNames = {"city_code", "subject_token_hash", "society_blind_hash"}
    )
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CityRiskSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city_code", nullable = false, length = 32)
    private String cityCode;

    @Column(name = "subject_token_hash", nullable = false, length = 64)
    private String subjectTokenHash;

    @Column(name = "society_blind_hash", nullable = false, length = 64)
    private String societyBlindHash;

    @Column(nullable = false, length = 40)
    private String category; // DELIVERY_IMPERSONATION, THEFT_FIR, ASSAULT_THREAT, LOCAL_DISPUTE

    @Column(nullable = false, length = 24)
    private String visibility; // CITY_ELIGIBLE, LOCAL_ONLY

    @Builder.Default
    @Column(name = "verified_by_mc", nullable = false)
    private boolean verifiedByMc = false;

    @Builder.Default
    @Column(name = "revoked_on_appeal", nullable = false)
    private boolean revokedOnAppeal = false;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
