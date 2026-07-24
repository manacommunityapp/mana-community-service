package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "food_subscription_customizations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodSubscriptionCustomization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private FoodSubscription subscription;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @Column(name = "meal_preference", length = 200)
    private String mealPreference;

    @Column(name = "exclude_ingredients", columnDefinition = "TEXT")
    private String excludeIngredients;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
