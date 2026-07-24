package com.manacommunity.api.vendor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "vms_vendor_service_pricing_tiers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmsVendorServicePricingTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private VmsVendorService service;

    @Column(name = "tier_name", nullable = false, length = 100)
    private String tierName;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(length = 30)
    private String unit;

    @Column(name = "min_quantity")
    @Builder.Default
    private Integer minQuantity = 1;

    @Column(name = "max_quantity")
    private Integer maxQuantity;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
