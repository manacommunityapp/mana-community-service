package com.manacommunity.api.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_move_line")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMoveLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "lot_id")
    private Long lotId;

    @Column(nullable = false)
    private Double quantity;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "location_dest_id", nullable = false)
    private Long locationDestId;

    @Column(name = "result_package_id")
    private Long resultPackageId;

    @Column(name = "move_id", nullable = false)
    private Long moveId;

    @Column(name = "picking_id")
    private Long pickingId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
