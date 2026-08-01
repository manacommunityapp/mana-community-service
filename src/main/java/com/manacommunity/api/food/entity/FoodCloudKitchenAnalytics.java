package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_cloud_kitchen_analytics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodCloudKitchenAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kitchen_id", nullable = false)
    private FoodCloudKitchen kitchen;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "total_orders")
    private Integer totalOrders;

    @Column(precision = 12, scale = 2)
    private BigDecimal revenue;

    @Column(name = "utilization_pct", precision = 5, scale = 2)
    private BigDecimal utilizationPct;

    @Column(name = "wastage_pct", precision = 5, scale = 2)
    private BigDecimal wastagePct;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
