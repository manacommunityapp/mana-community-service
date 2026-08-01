package com.manacommunity.api.cfbos.tax.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cfbos_tax_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_gstin", length = 15)
    private String communityGstin;

    @Column(name = "community_state_code", length = 2)
    private String communityStateCode;

    @Column(name = "is_gst_registered", nullable = false)
    @Builder.Default
    private Boolean isGstRegistered = false;

    @Column(name = "default_gst_rate", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal defaultGstRate = new BigDecimal("18.00");

    @Column(name = "default_cgst_rate", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal defaultCgstRate = new BigDecimal("9.00");

    @Column(name = "default_sgst_rate", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal defaultSgstRate = new BigDecimal("9.00");

    @Column(name = "financial_year_start_month", nullable = false)
    @Builder.Default
    private Integer financialYearStartMonth = 4;

    private Long updatedBy;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
