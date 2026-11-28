package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_payslips",
    uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "payroll_run_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrPayslip {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "employee_id", nullable = false)
    private HrEmployee employee;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "payroll_run_id", nullable = false)
    private HrPayrollRun payrollRun;
    @Column(name = "working_days") private Integer workingDays;
    @Column(name = "paid_days") private Integer paidDays;
    @Column(name = "loss_of_pay_days") @Builder.Default private Integer lossOfPayDays = 0;
    @Column(name = "basic_salary", precision = 14, scale = 2) @Builder.Default private BigDecimal basicSalary = BigDecimal.ZERO;
    @Column(name = "hra", precision = 14, scale = 2) @Builder.Default private BigDecimal hra = BigDecimal.ZERO;
    @Column(name = "da", precision = 14, scale = 2) @Builder.Default private BigDecimal da = BigDecimal.ZERO;
    @Column(name = "special_allowance", precision = 14, scale = 2) @Builder.Default private BigDecimal specialAllowance = BigDecimal.ZERO;
    @Column(name = "other_allowances", precision = 14, scale = 2) @Builder.Default private BigDecimal otherAllowances = BigDecimal.ZERO;
    @Column(name = "gross_salary", precision = 14, scale = 2) @Builder.Default private BigDecimal grossSalary = BigDecimal.ZERO;
    @Column(name = "pf_employee", precision = 14, scale = 2) @Builder.Default private BigDecimal pfEmployee = BigDecimal.ZERO;
    @Column(name = "pf_employer", precision = 14, scale = 2) @Builder.Default private BigDecimal pfEmployer = BigDecimal.ZERO;
    @Column(name = "esi_employee", precision = 14, scale = 2) @Builder.Default private BigDecimal esiEmployee = BigDecimal.ZERO;
    @Column(name = "esi_employer", precision = 14, scale = 2) @Builder.Default private BigDecimal esiEmployer = BigDecimal.ZERO;
    @Column(name = "professional_tax", precision = 14, scale = 2) @Builder.Default private BigDecimal professionalTax = BigDecimal.ZERO;
    @Column(name = "tds", precision = 14, scale = 2) @Builder.Default private BigDecimal tds = BigDecimal.ZERO;
    @Column(name = "other_deductions", precision = 14, scale = 2) @Builder.Default private BigDecimal otherDeductions = BigDecimal.ZERO;
    @Column(name = "total_deductions", precision = 14, scale = 2) @Builder.Default private BigDecimal totalDeductions = BigDecimal.ZERO;
    @Column(name = "net_salary", precision = 14, scale = 2) @Builder.Default private BigDecimal netSalary = BigDecimal.ZERO;
    @Column(name = "reimbursements_paid", precision = 14, scale = 2) @Builder.Default private BigDecimal reimbursementsPaid = BigDecimal.ZERO;
    @Column(name = "is_published", nullable = false) @Builder.Default private Boolean isPublished = false;
    @Column(name = "published_at") private LocalDateTime publishedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
