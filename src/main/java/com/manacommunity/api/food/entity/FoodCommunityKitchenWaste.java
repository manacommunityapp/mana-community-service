package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_community_kitchen_waste")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodCommunityKitchenWaste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kitchen_id", nullable = false)
    private FoodCommunityKitchen kitchen;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "meal_type", length = 20)
    private String mealType;

    @Column(name = "food_prepared_kg", precision = 10, scale = 2)
    private BigDecimal foodPreparedKg;

    @Column(name = "food_consumed_kg", precision = 10, scale = 2)
    private BigDecimal foodConsumedKg;

    @Column(name = "waste_kg", precision = 10, scale = 2)
    private BigDecimal wasteKg;

    @Column(name = "waste_type", length = 100)
    private String wasteType;

    @Column(name = "disposal_method", length = 100)
    private String disposalMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
