package com.manacommunity.api.insurance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_group_enquiry")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class GroupEnquiry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "enquiry_ref", nullable = false, unique = true, length = 24)
    private String enquiryRef;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "community_id")
    private Long communityId;
    @Column(name = "organisation_name", nullable = false, length = 200)
    private String organisationName;
    @Column(name = "organisation_type", nullable = false, length = 30)
    private String organisationType;
    @Enumerated(EnumType.STRING)
    @Column(name = "coverage_scope", nullable = false, length = 30)
    private InsuranceEnums.CoverageScope coverageScope;
    @Column(name = "coverage_sought", nullable = false, length = 40)
    private String coverageSought;
    @Column(name = "approx_members", nullable = false)
    private Integer approxMembers;
    @Column(length = 1000)
    private String requirements;
    @Column(name = "contact_method", nullable = false, length = 10)
    private String contactMethod;
    @Column(name = "consent_id", nullable = false)
    private Long consentId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InsuranceEnums.EnquiryStatus status = InsuranceEnums.EnquiryStatus.SUBMITTED;
    @Column(name = "assigned_to")
    private Long assignedTo;
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
