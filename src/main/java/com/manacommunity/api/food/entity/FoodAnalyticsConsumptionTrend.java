package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_analytics_consumption_trends")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodAnalyticsConsumptionTrend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column
    private LocalDate month;

    @Column(name = "total_orders")
    private Integer totalOrders;

    @Column(name = "total_revenue", precision = 15, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "avg_order_value", precision = 10, scale = 2)
    private BigDecimal avgOrderValue;

    @Column(name = "top_cuisines", columnDefinition = "TEXT")
    private String topCuisines;

    @Column(name = "top_restaurants", columnDefinition = "TEXT")
    private String topRestaurants;

    @Column(name = "veg_pct", precision = 5, scale = 2)
    private BigDecimal vegPct;

    @Column(name = "non_veg_pct", precision = 5, scale = 2)
    private BigDecimal nonVegPct;

    @Column(name = "subscription_count")
    private Integer subscriptionCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
