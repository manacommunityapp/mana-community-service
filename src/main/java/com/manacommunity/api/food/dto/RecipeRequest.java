package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class RecipeRequest {
    @NotBlank
    private String title;
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
    private List<RecipeIngredientRequest> ingredients;
}
