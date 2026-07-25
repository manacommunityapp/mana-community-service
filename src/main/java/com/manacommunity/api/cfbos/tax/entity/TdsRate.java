package com.manacommunity.api.cfbos.tax.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cfbos_tds_rate")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TdsRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tds_section_id", nullable = false)
    private TdsSection tdsSection;

    @Column(name = "payee_type", nullable = false, length = 20)
    private String payeeType;

    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal rate;

    @Column(precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal surcharge = BigDecimal.ZERO;

    @Column(precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal cess = BigDecimal.ZERO;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
