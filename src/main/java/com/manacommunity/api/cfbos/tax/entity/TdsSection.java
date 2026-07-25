package com.manacommunity.api.cfbos.tax.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cfbos_tds_section")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TdsSection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_code", nullable = false, unique = true, length = 10)
    private String sectionCode;

    @Column(nullable = false)
    private String description;

    @Column(name = "individual_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal individualRate;

    @Column(name = "company_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal companyRate;

    @Column(name = "threshold_amount", precision = 18, scale = 2)
    private BigDecimal thresholdAmount;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
