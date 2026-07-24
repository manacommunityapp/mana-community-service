package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_meal_plan_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodMealPlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private FoodMealPlan plan;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @Column(name = "meal_type", length = 50)
    private String mealType;

    @Column(name = "recipe_id")
    private Long recipeId;

    @Column(name = "item_name", length = 200)
    private String itemName;

    @Column
    private Integer calories;

    @Column(precision = 10, scale = 2)
    private BigDecimal protein;

    @Column(precision = 10, scale = 2)
    private BigDecimal carbs;

    @Column(precision = 10, scale = 2)
    private BigDecimal fat;

    @Column(name = "portion_size", length = 100)
    private String portionSize;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
