package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RecipeResponse {
    private Long id;
    private String title;
    private String slug;
    private String description;
    private String cuisineType;
    private String mealType;
    private String courseType;
    private String difficulty;
    private Integer prepTime;
    private Integer cookTime;
    private Integer servings;
    private Integer calories;
    private Integer protein;
    private Integer carbs;
    private Integer fat;
    private String imageUrl;
    private String videoUrl;
    private String instructions;
    private String tips;
    private String tags;
    private Boolean isVeg;
    private Boolean isVegan;
    private Boolean isGlutenFree;
    private String status;
    private List<IngredientResponse> ingredients;
    private BigDecimal avgRating;
    private Integer totalRatings;
    private Long authorId;
    private String authorName;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class IngredientResponse {
        private Long id;
        private String ingredientName;
        private String quantity;
        private String unit;
        private Boolean isOptional;
        private String substitute;
    }
}
