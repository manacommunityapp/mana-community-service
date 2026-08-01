package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_ai_demand_predictions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodAiDemandPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_type", length = 50)
    private String providerType;

    @Column(name = "provider_id")
    private Long providerId;

    @Column(name = "prediction_date")
    private LocalDate predictionDate;

    @Column(name = "predicted_orders")
    private Integer predictedOrders;

    @Column(name = "predicted_revenue", precision = 10, scale = 2)
    private BigDecimal predictedRevenue;

    @Column(precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(columnDefinition = "TEXT")
    private String factors;

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
