package com.manacommunity.api.kyc.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "kyc_verification_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycVerificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "household_member_id")
    private Long householdMemberId;

    @Column(name = "verification_method", nullable = false, length = 50)
    @Builder.Default
    private String verificationMethod = "MANUAL_REVIEW";

    @Column(name = "id_type", length = 50)
    private String idType;

    // SHA-256 hash of actual ID number; plaintext is never stored
    @Column(name = "id_number_hash", length = 100)
    private String idNumberHash;

    @Column(name = "document_url", length = 500)
    private String documentUrl;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    // Verification validity period (2 years from approval)
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "external_ref", length = 200)
    private String externalRef;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
