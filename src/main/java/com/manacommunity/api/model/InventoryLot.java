package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lot or serial number for tracked products.
 *
 * When product.tracking = LOT:  multiple units can share one lot name.
 * When product.tracking = SERIAL: each unit gets a unique lot name, and
 * qty per lot must always be exactly 1.
 */
@Entity
@Table(name = "inventory_lot",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_lot_name_product",
        columnNames = {"name", "product_id"}
    ),
    indexes = {
        @Index(name = "idx_inv_lot_product", columnList = "product_id"),
    }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "product")
public class InventoryLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Lot number or serial number string, e.g. "LOT-2026-001" or "SN-A7X9K". */
    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private InventoryProduct product;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
