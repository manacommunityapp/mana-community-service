package com.manacommunity.api.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_quant")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockQuant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(nullable = false)
    private Double quantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Double reservedQuantity;

    @Column(name = "lot_id")
    private Long lotId;

    @Column(name = "in_date", nullable = false)
    private LocalDateTime inDate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (inDate == null) {
            inDate = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
        if (quantity == null) {
            quantity = 0.0;
        }
        if (reservedQuantity == null) {
            reservedQuantity = 0.0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
