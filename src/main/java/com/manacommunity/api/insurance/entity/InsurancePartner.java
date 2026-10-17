package com.manacommunity.api.insurance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_partner")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InsurancePartner {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 40)
    private String code;
    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;
    @Column(name = "partner_type", nullable = false, length = 30)
    private String partnerType;
    @Column(name = "irdai_registration_no", length = 60)
    private String irdaiRegistrationNo;
    @Column(name = "authorisation_verified", nullable = false)
    @Builder.Default
    private boolean authorisationVerified = false;
    @Column(name = "authorisation_verified_at")
    private LocalDateTime authorisationVerifiedAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InsuranceEnums.PartnerStatus status = InsuranceEnums.PartnerStatus.PENDING_APPROVAL;
    @Column(name = "handoff_base_url", length = 500)
    private String handoffBaseUrl;
    @Column(name = "disclosure_text", length = 2000)
    private String disclosureText;
    @Column(name = "commercial_disclosure", length = 2000)
    private String commercialDisclosure;
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    @Column(name = "approved_by")
    private Long approvedBy;
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** A partner may only be surfaced to users when approved AND authorisation verified. */
    public boolean isSurfaceable() {
        return status == InsuranceEnums.PartnerStatus.ACTIVE && authorisationVerified;
    }
}
