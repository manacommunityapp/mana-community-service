package com.manacommunity.api.food.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MealPlanItemRequest {
    private String dayOfWeek;
    private String mealType;
    private Long recipeId;
    private String itemName;
    private Integer calories;
    private BigDecimal protein;
    private BigDecimal carbs;
    private BigDecimal fat;
    private String portionSize;
}
