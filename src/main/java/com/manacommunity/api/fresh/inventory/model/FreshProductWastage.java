package com.manacommunity.api.fresh.inventory.model;

import com.manacommunity.api.fresh.city.model.FreshFulfilmentLocation;
import com.manacommunity.api.freshkitchen.model.FoodProductionBatch;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fresh_product_wastage", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreshProductWastage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wastage_reference", unique = true, nullable = false, length = 50)
    private String wastageReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_product_id", nullable = false)
    private VendorProduct vendorProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private FoodProductionBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fulfilment_location_id")
    private FreshFulfilmentLocation fulfilmentLocation;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    @Builder.Default
    private String unitOfMeasure = "KG";

    @Column(name = "cost_price_per_unit", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal costPricePerUnit = BigDecimal.ZERO;

    @Column(name = "total_loss_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalLossAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "wastage_category", nullable = false, length = 50)
    private WastageCategory wastageCategory;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "disposal_method", length = 100)
    private String disposalMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_user_id", nullable = false)
    private AppUser recordedByUser;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;
}
