package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * The stock ledger. Each row represents "how much of product X is at location Y".
 * Double-entry stock moves update two quant rows: subtract from source, add to dest.
 *
 * The (product_id, location_id) pair is unique — there's exactly one quant row
 * per product per location. This is the single source of truth for on-hand quantities.
 */
@Entity
@Table(name = "inventory_stock_quant",
    indexes = {
        @Index(name = "idx_inv_sq_product",  columnList = "product_id"),
        @Index(name = "idx_inv_sq_location", columnList = "location_id"),
    }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"product", "location", "lot"})
public class InventoryStockQuant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private InventoryProduct product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private InventoryLocation location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private InventoryLot lot;

    /** Actual on-hand quantity at this location. */
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ZERO;

    /** Quantity reserved by confirmed (but not yet done) pickings. */
    @Column(name = "reserved_qty", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal reservedQty = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }
}
