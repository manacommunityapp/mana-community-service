package com.manacommunity.api.cfbos.charge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cfbos_tier_config")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TierConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slab_config_id")
    private SlabConfig slabConfig;
    @Column(name = "tier_from", precision = 18, scale = 4, nullable = false)
    private BigDecimal tierFrom;
    @Column(name = "tier_to", precision = 18, scale = 4)
    private BigDecimal tierTo;
    @Column(precision = 18, scale = 4, nullable = false)
    private BigDecimal rate;
    @Column(name = "fixed_charge", precision = 18, scale = 2, nullable = false) @Builder.Default
    private BigDecimal fixedCharge = BigDecimal.ZERO;
    @Column(name = "tier_order", nullable = false)
    private Integer tierOrder;
}
