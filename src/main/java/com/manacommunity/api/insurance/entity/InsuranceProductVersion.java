package com.manacommunity.api.insurance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_product_version")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InsuranceProductVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "product_id", nullable = false)
    private Long productId;
    @Column(name = "version_no", nullable = false)
    private Integer versionNo;
    @Column(name = "data_source", nullable = false, length = 20)
    private String dataSource;
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private InsuranceEnums.VerificationStatus verificationStatus = InsuranceEnums.VerificationStatus.UNVERIFIED;
    @Column(name = "brochure_url", length = 500)
    private String brochureUrl;
    @Column(name = "policy_wording_url", length = 500)
    private String policyWordingUrl;
    @Column(name = "eligibility_json", columnDefinition = "TEXT")
    private String eligibilityJson;
    @Column(name = "coverage_json", columnDefinition = "TEXT")
    private String coverageJson;
    @Column(name = "benefits_json", columnDefinition = "TEXT")
    private String benefitsJson;
    @Column(name = "waiting_periods_json", columnDefinition = "TEXT")
    private String waitingPeriodsJson;
    @Column(name = "exclusions_json", columnDefinition = "TEXT")
    private String exclusionsJson;
    @Column(name = "network_json", columnDefinition = "TEXT")
    private String networkJson;
    @Column(name = "disclosures_json", columnDefinition = "TEXT")
    private String disclosuresJson;
    @Column(name = "submitted_by", nullable = false)
    private Long submittedBy;
    @Column(name = "approved_by")
    private Long approvedBy;
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
