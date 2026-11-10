package com.manacommunity.api.fresh.processing.model;

import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import com.manacommunity.api.vendor.entity.VmsVendor;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "fresh_processing_batch", schema = "manacommunity")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FreshProcessingBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_number", nullable = false, unique = true, length = 80)
    private String batchNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id")
    private FreshProcessingRecipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vms_vendor_id", nullable = false)
    private VmsVendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_product_id", nullable = false)
    private VendorProduct vendorProduct;

    @Column(name = "production_date", nullable = false)
    private LocalDate productionDate;

    @Column(name = "raw_material_input_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal rawMaterialInputQty;

    @Column(name = "actual_output_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal actualOutputQty;

    @Column(name = "actual_yield_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal actualYieldPercent;

    @Column(name = "wastage_qty", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal wastageQty = BigDecimal.ZERO;

    @Column(name = "wastage_reason")
    private String wastageReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private BatchStatus status = BatchStatus.IN_PRODUCTION;

    @Column(name = "storage_temp_celsius", precision = 4, scale = 1)
    private BigDecimal storageTempCelsius;

    @Column(name = "expiry_timestamp", nullable = false)
    private Instant expiryTimestamp;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }
}