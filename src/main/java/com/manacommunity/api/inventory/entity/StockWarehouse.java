package com.manacommunity.api.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_warehouse")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockWarehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "partner_id")
    private Long partnerId;

    @Column(name = "lot_stock_id")
    private Long lotStockId;

    @Column(name = "reception_steps", nullable = false, length = 20)
    private String receptionSteps;

    @Column(name = "delivery_steps", nullable = false, length = 20)
    private String deliverySteps;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (receptionSteps == null) {
            receptionSteps = "1_step";
        }
        if (deliverySteps == null) {
            deliverySteps = "1_step";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
