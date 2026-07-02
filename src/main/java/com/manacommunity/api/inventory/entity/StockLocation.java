package com.manacommunity.api.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_location")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "complete_name", nullable = false, unique = true, length = 255)
    private String completeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage", nullable = false, length = 30)
    private LocationUsage usage;

    @Column(name = "location_id")
    private Long locationId;

    @Column(length = 40)
    private String barcode;

    @Column(name = "warehouse_id")
    private Long warehouseId;

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
