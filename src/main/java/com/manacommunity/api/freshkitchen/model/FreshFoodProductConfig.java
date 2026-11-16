package com.manacommunity.api.freshkitchen.model;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "fresh_food_product_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class FreshFoodProductConfig extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true, length = 64)
    private String productId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FoodCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_mode", nullable = false, length = 50)
    @Builder.Default
    private ExecutionMode executionMode = ExecutionMode.DAILY_BASKET;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String unit = "PACK";

    @Column(name = "pack_size", length = 50)
    private String packSize;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "shelf_life_hours", nullable = false)
    @Builder.Default
    private Integer shelfLifeHours = 48;

    @Column(name = "storage_temp_min", nullable = false, precision = 4, scale = 1)
    @Builder.Default
    private BigDecimal storageTempMin = new BigDecimal("2.0");

    @Column(name = "storage_temp_max", nullable = false, precision = 4, scale = 1)
    @Builder.Default
    private BigDecimal storageTempMax = new BigDecimal("4.0");

    @Column(name = "min_order_lead_hours", nullable = false)
    @Builder.Default
    private Integer minOrderLeadHours = 8;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
