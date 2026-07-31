package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_recipe_ingredients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodRecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private FoodRecipe recipe;

    @Column(name = "ingredient_name", length = 200)
    private String ingredientName;

    @Column(precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(length = 50)
    private String unit;

    @Column(name = "is_optional")
    @Builder.Default
    private Boolean isOptional = false;

    @Column(length = 200)
    private String substitute;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
