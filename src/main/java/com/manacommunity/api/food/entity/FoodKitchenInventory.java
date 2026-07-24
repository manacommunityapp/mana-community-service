package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_kitchen_inventories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodKitchenInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "kitchen_type", length = 30)
    private KitchenType kitchenType;

    @Column(name = "kitchen_id")
    private Long kitchenId;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(length = 100)
    private String category;

    @Column(length = 50)
    private String unit;

    @Column(name = "current_stock", precision = 10, scale = 2)
    private BigDecimal currentStock;

    @Column(name = "min_stock", precision = 10, scale = 2)
    private BigDecimal minStock;

    @Column(name = "max_stock", precision = 10, scale = 2)
    private BigDecimal maxStock;

    @Column(name = "reorder_level", precision = 10, scale = 2)
    private BigDecimal reorderLevel;

    @Column(name = "unit_cost", precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "last_restocked_at")
    private LocalDateTime lastRestockedAt;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "storage_location", length = 200)
    private String storageLocation;

    @Column(length = 100)
    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

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
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum KitchenType { RESTAURANT, HOME_CHEF, CLOUD_KITCHEN, COMMUNITY_KITCHEN }
}
