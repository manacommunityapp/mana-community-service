package com.manacommunity.api.vendor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "vms_contract_sla")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmsContractSla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private VmsContract contract;

    @Column(name = "metric_name", nullable = false, length = 150)
    private String metricName;

    @Column(name = "target_value", nullable = false, length = 100)
    private String targetValue;

    @Column(length = 30)
    private String unit;

    @Column(name = "penalty_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Column(name = "measurement_freq", length = 30)
    private String measurementFreq;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
