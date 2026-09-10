package com.manacommunity.api.marketplace.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "marketplace_group_order_tiers", indexes = {
    @Index(name = "idx_mkt_tier_grp", columnList = "group_order_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketGroupOrderTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_order_id", nullable = false)
    @JsonIgnoreProperties({"discountTiers", "participants"})
    private MarketGroupOrder groupOrder;

    @Column(name = "min_quantity", nullable = false)
    private Integer minQuantity;

    @Column(name = "discounted_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountedPrice;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;
}
