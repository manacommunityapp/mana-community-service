package com.cpn.domain.freelance.model;

import com.cpn.domain.common.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "freelance_projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreelanceProject extends TenantAwareEntity {

    @Column(nullable = false)
    private UUID clientId;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    private String budgetType; // FIXED, HOURLY
    private BigDecimal budget;
    private String skillsRequired;
    private String status; // OPEN, IN_PROGRESS, COMPLETED
}
