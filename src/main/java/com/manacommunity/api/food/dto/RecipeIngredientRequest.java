package com.manacommunity.api.food.dto;

import lombok.Data;

@Data
public class RecipeIngredientRequest {
    private String ingredientName;
    private String quantity;
    private String unit;
    private Boolean isOptional;
    private String substitute;
}
