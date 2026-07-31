package com.cpn.domain.jobs.model;

import com.cpn.domain.common.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job extends TenantAwareEntity {

    @Column(nullable = false)
    private UUID companyId;

    private String companyName;

    @Column(nullable = false)
    private UUID postedBy;

    @Column(nullable = false)
    private String title;

    @Column(length = 4000)
    private String description;

    private String jobType;  // FULL_TIME, PART_TIME, CONTRACT
    private String workMode; // REMOTE, HYBRID, ONSITE
    private Integer experienceMin;
    private Integer experienceMax;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String currency;
    private String locationCity;
    private String industry;
    private boolean isActive;
}
