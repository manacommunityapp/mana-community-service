package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_payroll_runs",
    uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "payroll_month", "payroll_year"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrPayrollRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @Column(name = "payroll_month", nullable = false) private Integer payrollMonth;
    @Column(name = "payroll_year", nullable = false) private Integer payrollYear;
    @Column(name = "status", nullable = false, length = 20) @Builder.Default private String status = "DRAFT";
    @Column(name = "total_gross") @Builder.Default private Double totalGross = 0.0;
    @Column(name = "total_deductions") @Builder.Default private Double totalDeductions = 0.0;
    @Column(name = "total_net") @Builder.Default private Double totalNet = 0.0;
    @Column(name = "employee_count") @Builder.Default private Integer employeeCount = 0;
    @Column(name = "processed_by") private Long processedBy;
    @Column(name = "processed_at") private LocalDateTime processedAt;
    @Column(name = "approved_by") private Long approvedBy;
    @Column(name = "approved_at") private LocalDateTime approvedAt;
    @Column(name = "remarks", length = 500) private String remarks;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
