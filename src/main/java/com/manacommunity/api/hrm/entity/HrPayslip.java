package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
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
    @Column(name = "basic_salary") @Builder.Default private Double basicSalary = 0.0;
    @Column(name = "hra") @Builder.Default private Double hra = 0.0;
    @Column(name = "da") @Builder.Default private Double da = 0.0;
    @Column(name = "special_allowance") @Builder.Default private Double specialAllowance = 0.0;
    @Column(name = "other_allowances") @Builder.Default private Double otherAllowances = 0.0;
    @Column(name = "gross_salary") @Builder.Default private Double grossSalary = 0.0;
    @Column(name = "pf_employee") @Builder.Default private Double pfEmployee = 0.0;
    @Column(name = "pf_employer") @Builder.Default private Double pfEmployer = 0.0;
    @Column(name = "esi_employee") @Builder.Default private Double esiEmployee = 0.0;
    @Column(name = "esi_employer") @Builder.Default private Double esiEmployer = 0.0;
    @Column(name = "professional_tax") @Builder.Default private Double professionalTax = 0.0;
    @Column(name = "tds") @Builder.Default private Double tds = 0.0;
    @Column(name = "other_deductions") @Builder.Default private Double otherDeductions = 0.0;
    @Column(name = "total_deductions") @Builder.Default private Double totalDeductions = 0.0;
    @Column(name = "net_salary") @Builder.Default private Double netSalary = 0.0;
    @Column(name = "reimbursements_paid") @Builder.Default private Double reimbursementsPaid = 0.0;
    @Column(name = "is_published", nullable = false) @Builder.Default private Boolean isPublished = false;
    @Column(name = "published_at") private LocalDateTime publishedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
