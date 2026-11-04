package com.manacommunity.api.fresh.processing.model;

import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import com.manacommunity.api.vendor.entity.VmsVendor;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fresh_processing_recipe", schema = "manacommunity")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FreshProcessingRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vms_vendor_id", nullable = false)
    private VmsVendor vendor;

    @Column(name = "recipe_code", nullable = false, unique = true, length = 60)
    private String recipeCode;

    @Column(name = "recipe_name", nullable = false, length = 150)
    private String recipeName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "output_product_id", nullable = false)
    private VendorProduct outputProduct;

    @Column(name = "target_output_unit", nullable = false, length = 30)
    private String targetOutputUnit;

    @Column(name = "expected_yield_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal expectedYieldPercent = BigDecimal.valueOf(100.00);

    @Column(name = "storage_temp_min_celsius", nullable = false, precision = 4, scale = 1)
    @Builder.Default
    private BigDecimal storageTempMinCelsius = BigDecimal.valueOf(2.0);

    @Column(name = "storage_temp_max_celsius", nullable = false, precision = 4, scale = 1)
    @Builder.Default
    private BigDecimal storageTempMaxCelsius = BigDecimal.valueOf(4.0);

    @Column(name = "shelf_life_hours", nullable = false)
    @Builder.Default
    private Integer shelfLifeHours = 24;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }
}