package com.manacommunity.api.groupbuy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "group_deal_tier")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDealTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id", nullable = false)
    private GroupDeal deal;

    @Column(nullable = false)
    private Integer minQty;

    private Integer maxQty;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerUnit;

    @Column(length = 80)
    private String label;

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
