package com.manacommunity.api.homeservice.model.entity;

import com.manacommunity.api.homeservice.model.enums.HomeServiceWorkerType;
import com.manacommunity.api.homeservice.model.enums.HomeServiceVerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "home_service_workers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeServiceWorkerEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "community_id", nullable = false, length = 64)
    private String communityId;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "worker_type", nullable = false, length = 30)
    @Builder.Default
    private HomeServiceWorkerType workerType = HomeServiceWorkerType.COMMUNITY_WORKER;

    @Column(name = "agency_name", length = 150)
    private String agencyName;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(name = "primary_phone", nullable = false, length = 20)
    private String primaryPhone;

    @Column(name = "alternate_phone", length = 20)
    private String alternatePhone;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @Column(name = "id_proof_type", length = 50)
    private String idProofType;

    @Column(name = "id_proof_number", length = 100)
    private String idProofNumber;

    @Column(name = "id_proof_document_url")
    private String idProofDocumentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    @Builder.Default
    private HomeServiceVerificationStatus verificationStatus = HomeServiceVerificationStatus.PENDING;

    @Column(name = "police_verified")
    @Builder.Default
    private Boolean policeVerified = false;

    @Column(name = "community_verified")
    @Builder.Default
    private Boolean communityVerified = false;

    @Column(name = "security_verified")
    @Builder.Default
    private Boolean securityVerified = false;

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.valueOf(5.0);

    @Column(name = "total_reviews")
    @Builder.Default
    private Integer totalReviews = 0;

    @Column(name = "experience_years")
    @Builder.Default
    private Integer experienceYears = 0;

    @Column(length = 250)
    private String languages;

    @Column(name = "emergency_contact_name", length = 100)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
