package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_kitchen_inventory_forecasts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodKitchenInventoryForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kitchen_type", length = 50)
    private String kitchenType;

    @Column(name = "kitchen_id")
    private Long kitchenId;

    @Column(name = "item_name", length = 200)
    private String itemName;

    @Column(name = "forecast_date")
    private LocalDate forecastDate;

    @Column(name = "predicted_demand", precision = 10, scale = 2)
    private BigDecimal predictedDemand;

    @Column(precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
