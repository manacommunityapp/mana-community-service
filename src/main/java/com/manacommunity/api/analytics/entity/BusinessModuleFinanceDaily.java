package com.manacommunity.api.analytics.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "business_module_finance_daily",
        uniqueConstraints = @UniqueConstraint(name = "uq_business_module_finance_daily", columnNames = {"module_code", "report_date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessModuleFinanceDaily {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module_code", nullable = false, length = 60)
    private String moduleCode;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "commission_revenue", nullable = false, precision = 14, scale = 2)
    private BigDecimal commissionRevenue;

    @Column(name = "direct_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal directCost;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate protected void onWrite() { updatedAt = LocalDateTime.now(); }
}
