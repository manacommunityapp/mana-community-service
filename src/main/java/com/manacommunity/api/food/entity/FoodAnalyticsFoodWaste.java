package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_analytics_food_waste")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodAnalyticsFoodWaste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column
    private LocalDate month;

    @Column(name = "total_waste_kg", precision = 10, scale = 2)
    private BigDecimal totalWasteKg;

    @Column(name = "waste_by_source", columnDefinition = "TEXT")
    private String wasteBySource;

    @Column(name = "cost_impact", precision = 10, scale = 2)
    private BigDecimal costImpact;

    @Column(name = "reduction_pct", precision = 5, scale = 2)
    private BigDecimal reductionPct;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
