package com.manacommunity.api.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_move")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMove {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_uom_qty", nullable = false)
    private Double productUomQty;

    @Column(nullable = false)
    private Double quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MoveState state;

    @Column(name = "picking_id")
    private Long pickingId;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (state == null) {
            state = MoveState.draft;
        }
        if (quantity == null) {
            quantity = 0.0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
