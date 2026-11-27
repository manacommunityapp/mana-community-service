package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_contractor_agency")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrContractorAgency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "agency_name", nullable = false, length = 150)
    private String agencyName;

    @Column(name = "agency_code", nullable = false, unique = true, length = 50)
    private String agencyCode;

    @Column(name = "contact_person", nullable = false, length = 100)
    private String contactPerson;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "gst_number", length = 30)
    private String gstNumber;

    @Column(name = "billing_rate_per_hour", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal billingRatePerHour = BigDecimal.valueOf(180.00);

    @Column(name = "max_headcount_quota")
    @Builder.Default
    private Integer maxHeadcountQuota = 20;

    @Column(name = "compliance_status", length = 30)
    @Builder.Default
    private String complianceStatus = "COMPLIANT";

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
