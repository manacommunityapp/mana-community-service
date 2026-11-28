package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hr_contractor_worker")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrContractorWorker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private HrContractorAgency agency;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "worker_code", nullable = false, unique = true, length = 50)
    private String workerCode;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "category", length = 60)
    private String category;

    @Column(name = "deployed_site", length = 120)
    private String deployedSite;

    @Column(name = "shift_name", length = 60)
    private String shiftName;

    @Column(name = "face_embedding_hash", length = 256)
    private String faceEmbeddingHash;

    @Column(name = "kyc_verified")
    @Builder.Default
    private Boolean kycVerified = false;

    @Column(name = "dpdp_consent_signed")
    @Builder.Default
    private Boolean dpdpConsentSigned = false;

    @Column(name = "safety_training_passed")
    @Builder.Default
    private Boolean safetyTrainingPassed = false;

    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "last_punch_time")
    private LocalDateTime lastPunchTime;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
