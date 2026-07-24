package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_resident_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodResidentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "diet_type", length = 30)
    private DietType dietType;

    @Column(name = "calorie_goal")
    private Integer calorieGoal;

    @Column(name = "protein_goal", precision = 10, scale = 2)
    private BigDecimal proteinGoal;

    @Column(name = "weight_goal", precision = 10, scale = 2)
    private BigDecimal weightGoal;

    @Column(name = "health_goal", length = 500)
    private String healthGoal;

    @Column(name = "fitness_goal", length = 500)
    private String fitnessGoal;

    @Column(name = "water_intake_goal")
    private Integer waterIntakeGoal;

    @Column(name = "coffee_limit")
    private Integer coffeeLimit;

    @Column(name = "daily_nutrition_score", precision = 10, scale = 2)
    private BigDecimal dailyNutritionScore;

    @Column(name = "ai_lifestyle_score", precision = 10, scale = 2)
    private BigDecimal aiLifestyleScore;

    @Column(precision = 10, scale = 2)
    private BigDecimal bmi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum DietType { VEGETARIAN, NON_VEGETARIAN, VEGAN, JAIN, HALAL, KOSHER, EGGITARIAN, GLUTEN_FREE, LACTOSE_FREE }
}
