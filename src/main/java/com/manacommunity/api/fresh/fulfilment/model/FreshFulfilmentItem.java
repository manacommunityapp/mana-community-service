package com.manacommunity.api.fresh.fulfilment.model;

import com.manacommunity.api.commerce.core.model.CommerceOrderItem;
import com.manacommunity.api.freshkitchen.model.FoodProductionBatch;
import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fresh_fulfilment_item", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreshFulfilmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fulfilment_group_id", nullable = false)
    private FreshFulfilmentGroup fulfilmentGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private CommerceOrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_product_id")
    private VendorProduct vendorProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private FoodProductionBatch batch;

    @Column(name = "requested_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal requestedQuantity;

    @Column(name = "picked_quantity", precision = 12, scale = 3)
    private BigDecimal pickedQuantity;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    @Builder.Default
    private String unitOfMeasure = "UNIT";

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "billed_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal billedPrice = BigDecimal.ZERO;

    @Column(name = "is_weight_adjusted", nullable = false)
    @Builder.Default
    private Boolean isWeightAdjusted = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private FulfilmentItemStatus status = FulfilmentItemStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
