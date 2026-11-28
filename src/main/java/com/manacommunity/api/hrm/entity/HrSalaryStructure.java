package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
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
    @Column(name = "ctc_annual", nullable = false, precision = 14, scale = 2) private BigDecimal ctcAnnual;
    @Column(name = "basic_monthly", nullable = false, precision = 14, scale = 2) private BigDecimal basicMonthly;
    @Column(name = "hra_monthly", precision = 14, scale = 2) @Builder.Default private BigDecimal hraMonthly = BigDecimal.ZERO;
    @Column(name = "da_monthly", precision = 14, scale = 2) @Builder.Default private BigDecimal daMonthly = BigDecimal.ZERO;
    @Column(name = "special_allowance_monthly", precision = 14, scale = 2) @Builder.Default private BigDecimal specialAllowanceMonthly = BigDecimal.ZERO;
    @Column(name = "other_allowances_monthly", precision = 14, scale = 2) @Builder.Default private BigDecimal otherAllowancesMonthly = BigDecimal.ZERO;
    @Column(name = "pf_employee_monthly", precision = 14, scale = 2) @Builder.Default private BigDecimal pfEmployeeMonthly = BigDecimal.ZERO;
    @Column(name = "pf_employer_monthly", precision = 14, scale = 2) @Builder.Default private BigDecimal pfEmployerMonthly = BigDecimal.ZERO;
    @Column(name = "esi_employee_monthly", precision = 14, scale = 2) @Builder.Default private BigDecimal esiEmployeeMonthly = BigDecimal.ZERO;
    @Column(name = "esi_employer_monthly", precision = 14, scale = 2) @Builder.Default private BigDecimal esiEmployerMonthly = BigDecimal.ZERO;
    @Column(name = "professional_tax_monthly", precision = 14, scale = 2) @Builder.Default private BigDecimal professionalTaxMonthly = BigDecimal.ZERO;
    @Column(name = "tds_monthly", precision = 14, scale = 2) @Builder.Default private BigDecimal tdsMonthly = BigDecimal.ZERO;
    @Column(name = "effective_from", nullable = false) private LocalDate effectiveFrom;
    @Column(name = "effective_to") private LocalDate effectiveTo;
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
