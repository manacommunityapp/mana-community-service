package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_cloud_kitchen_productions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodCloudKitchenProduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kitchen_id", nullable = false)
    private FoodCloudKitchen kitchen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private FoodCloudKitchenBrand brand;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "planned_quantity")
    private Integer plannedQuantity;

    @Column(name = "actual_quantity")
    private Integer actualQuantity;

    private Integer wastage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
