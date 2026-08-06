package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_restaurant_analytics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodRestaurantAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private FoodRestaurant restaurant;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "total_orders")
    private Integer totalOrders;

    @Column(name = "total_revenue", precision = 12, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "avg_order_value", precision = 10, scale = 2)
    private BigDecimal avgOrderValue;

    @Column(name = "new_customers")
    private Integer newCustomers;

    @Column(name = "repeat_customers")
    private Integer repeatCustomers;

    @Column(name = "avg_preparation_time")
    private Integer avgPreparationTime;

    @Column(name = "avg_delivery_time")
    private Integer avgDeliveryTime;

    @Column(name = "cancellation_rate", precision = 5, scale = 2)
    private BigDecimal cancellationRate;

    @Column(name = "rating_avg", precision = 3, scale = 2)
    private BigDecimal ratingAvg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
