package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bulk_buy_campaigns")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkBuyCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private ProcurementCategory category = ProcurementCategory.OTHER;

    @Column(length = 40)
    private String unit;

    @Column(name = "min_order_quantity", nullable = false)
    @Builder.Default
    private Integer minOrderQuantity = 1;

    @Column(name = "target_quantity")
    private Integer targetQuantity;

    @Column(name = "current_quantity")
    @Builder.Default
    private Integer currentQuantity = 0;

    // Stored as "minQty1:price1,minQty2:price2,..." e.g. "100:450.00,250:425.00,500:400.00"
    @Column(name = "price_tiers", length = 500)
    private String priceTiers;

    @Column(name = "retail_price", precision = 12, scale = 2)
    private BigDecimal retailPrice;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "supplier_name", length = 200)
    private String supplierName;

    @Column(name = "deadline_date", nullable = false)
    private LocalDate deadlineDate;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BulkBuyCampaignStatus status = BulkBuyCampaignStatus.DRAFT;

    @Column(name = "commitment_count")
    @Builder.Default
    private Integer commitmentCount = 0;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
