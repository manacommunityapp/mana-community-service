package com.manacommunity.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_move_line", indexes = {
    @Index(name = "idx_inv_ml_picking", columnList = "picking_id"),
    @Index(name = "idx_inv_ml_product", columnList = "product_id"),
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"picking", "product", "location", "locationDest", "lot"})
public class InventoryMoveLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "picking_id", nullable = false)
    @JsonIgnore
    private InventoryPicking picking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private InventoryProduct product;

    /** Demand quantity. */
    @Column(name = "product_qty", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal productQty = BigDecimal.ZERO;

    /** Actually processed quantity. */
    @Column(name = "qty_done", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal qtyDone = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private InventoryLocation location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_dest_id", nullable = false)
    private InventoryLocation locationDest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private InventoryLot lot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
