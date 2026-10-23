package com.manacommunity.api.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable audit ledger of all inventory movements.
 * Every transaction (receipt, issue, internal move, adjustment, scrap, commerce reservation/fulfillment)
 * records a permanent entry here with previous and resulting balances.
 */
@Entity
@Table(name = "inventory_stock_ledger_entry", indexes = {
    @Index(name = "idx_inv_ledger_prod_loc", columnList = "product_id, location_id"),
    @Index(name = "idx_inv_ledger_community", columnList = "community_id"),
    @Index(name = "idx_inv_ledger_ref", columnList = "reference_type, reference_id"),
    @Index(name = "idx_inv_ledger_created_at", columnList = "created_at DESC")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"product", "location", "lot", "community", "createdByUser"})
public class StockLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private InventoryProduct product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private InventoryLocation location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private InventoryLot lot;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 40)
    private MovementType movementType;

    @Column(name = "reference_type", length = 40)
    private String referenceType;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    /** Quantity delta (positive for stock increase, negative for stock reduction). */
    @Column(name = "quantity_change", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityChange;

    /** Resulting running balance at this location post-movement. */
    @Column(name = "resulting_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal resultingBalance;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(name = "total_value", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalValue = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdByUser;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum MovementType {
        RECEIPT,
        DELIVERY,
        INTERNAL_TRANSFER,
        ADJUSTMENT,
        SCRAP,
        COMMERCE_RESERVATION,
        COMMERCE_FULFILLMENT,
        RETURN,
        OPENING_BALANCE
    }
}
