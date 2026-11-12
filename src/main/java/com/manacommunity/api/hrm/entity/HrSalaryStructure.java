package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_salary_structures",
    uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "effective_from"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrSalaryStructure {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "employee_id", nullable = false)
    private HrEmployee employee;
    @Column(name = "ctc_annual", nullable = false) private Double ctcAnnual;
    @Column(name = "basic_monthly", nullable = false) private Double basicMonthly;
    @Column(name = "hra_monthly") @Builder.Default private Double hraMonthly = 0.0;
    @Column(name = "da_monthly") @Builder.Default private Double daMonthly = 0.0;
    @Column(name = "special_allowance_monthly") @Builder.Default private Double specialAllowanceMonthly = 0.0;
    @Column(name = "other_allowances_monthly") @Builder.Default private Double otherAllowancesMonthly = 0.0;
    @Column(name = "pf_employee_monthly") @Builder.Default private Double pfEmployeeMonthly = 0.0;
    @Column(name = "pf_employer_monthly") @Builder.Default private Double pfEmployerMonthly = 0.0;
    @Column(name = "esi_employee_monthly") @Builder.Default private Double esiEmployeeMonthly = 0.0;
    @Column(name = "esi_employer_monthly") @Builder.Default private Double esiEmployerMonthly = 0.0;
    @Column(name = "professional_tax_monthly") @Builder.Default private Double professionalTaxMonthly = 0.0;
    @Column(name = "tds_monthly") @Builder.Default private Double tdsMonthly = 0.0;
    @Column(name = "effective_from", nullable = false) private LocalDate effectiveFrom;
    @Column(name = "effective_to") private LocalDate effectiveTo;
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
