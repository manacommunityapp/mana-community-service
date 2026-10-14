package com.manacommunity.api.safety.model;

import com.manacommunity.api.safety.dto.TrustAndSafetyDtos.LocationConsentPurpose;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "user_location_consent",
    indexes = {
        @Index(name = "idx_loc_consent_user", columnList = "user_id"),
        @Index(name = "idx_loc_consent_purpose", columnList = "user_id, purpose")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LocationConsentPurpose purpose;

    @Column(name = "is_granted", nullable = false)
    @Builder.Default
    private Boolean isGranted = false;

    @Column(name = "consent_version", nullable = false, length = 20)
    @Builder.Default
    private String consentVersion = "DPDP_2023_V1";

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "granted_at")
    private Instant grantedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;
}