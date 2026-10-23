package com.manacommunity.api.wellness.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMealAnalysisResponse {
    private List<MealItemDto> items;
    private Integer totalCalories;
    private BigDecimal totalProtein;
    private BigDecimal totalCarbs;
    private BigDecimal totalFat;
    private BigDecimal totalFiber;
    private BigDecimal confidence;
    private String suggestions;
    private BigDecimal junkFoodScore;
    private Boolean isJunkFood;
}
