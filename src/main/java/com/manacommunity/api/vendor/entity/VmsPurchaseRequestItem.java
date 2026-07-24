package com.manacommunity.api.vendor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "vms_purchase_request_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmsPurchaseRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private VmsPurchaseRequest request;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(length = 500)
    private String specification;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(length = 30)
    private String unit;

    @Column(name = "estimated_price", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal estimatedPrice = BigDecimal.ZERO;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
