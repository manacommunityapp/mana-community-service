package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_expense_claims")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrExpenseClaim {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "employee_id", nullable = false)
    private HrEmployee employee;
    @Column(name = "claim_title", nullable = false, length = 200) private String claimTitle;
    @Column(name = "claim_type", nullable = false, length = 30) private String claimType;
    @Column(name = "total_amount", nullable = false) @Builder.Default private Double totalAmount = 0.0;
    @Column(name = "approved_amount") private Double approvedAmount;
    @Column(name = "claim_date", nullable = false) private LocalDate claimDate;
    @Column(name = "description", columnDefinition = "TEXT") private String description;
    @Column(name = "status", nullable = false, length = 20) @Builder.Default private String status = "DRAFT";
    @Column(name = "approved_by") private Long approvedBy;
    @Column(name = "approved_at") private LocalDateTime approvedAt;
    @Column(name = "rejection_reason", length = 500) private String rejectionReason;
    @Column(name = "reimbursed_in_payroll_id") private Long reimbursedInPayrollId;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
