package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_employment_history")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrEmploymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private HrEmployee employee;

    @Column(name = "change_type", nullable = false, length = 50)
    private String changeType;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "previous_designation", length = 150)
    private String previousDesignation;

    @Column(name = "new_designation", length = 150)
    private String newDesignation;

    @Column(name = "previous_department", length = 150)
    private String previousDepartment;

    @Column(name = "new_department", length = 150)
    private String newDepartment;

    @Column(name = "previous_branch", length = 150)
    private String previousBranch;

    @Column(name = "new_branch", length = 150)
    private String newBranch;

    @Column(name = "previous_reporting_manager_id")
    private Long previousReportingManagerId;

    @Column(name = "new_reporting_manager_id")
    private Long newReportingManagerId;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
