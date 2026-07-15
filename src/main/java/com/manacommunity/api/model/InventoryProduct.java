package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_product", indexes = {
    @Index(name = "idx_inv_product_category",  columnList = "category_id"),
    @Index(name = "idx_inv_product_community", columnList = "community_id"),
    @Index(name = "idx_inv_product_code",      columnList = "default_code"),
    @Index(name = "idx_inv_product_barcode",   columnList = "barcode"),
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"category", "community"})
public class InventoryProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    /** SKU / internal reference code. */
    @Column(name = "default_code", length = 50)
    private String defaultCode;

    @Column(length = 100)
    private String barcode;

    /** Sales price. */
    @Column(name = "list_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal listPrice = BigDecimal.ZERO;

    /** Cost / standard price for valuation. */
    @Column(name = "standard_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal standardPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProductType type = ProductType.STORABLE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private TrackingType tracking = TrackingType.NONE;

    @Column(name = "valuation_method", length = 20)
    @Builder.Default
    private String valuationMethod = "STANDARD";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private InventoryCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }

    /**
     * Transient field — populated by the service from stock_quant aggregation,
     * NOT stored in this table. Matches the frontend's Product.qtyAvailable.
     */
    @Transient
    private BigDecimal qtyAvailable;

    // ─── Enums ───────────────────────────────────────────────────────────

    public enum ProductType { STORABLE, CONSUMABLE, SERVICE }
    public enum TrackingType { NONE, LOT, SERIAL }
}
