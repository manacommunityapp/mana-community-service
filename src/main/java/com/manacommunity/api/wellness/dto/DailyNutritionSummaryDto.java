package com.manacommunity.api.wellness.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyNutritionSummaryDto {
    private LocalDate date;
    private Integer totalCalories;
    private Integer calorieTarget;
    private BigDecimal totalProtein;
    private BigDecimal totalCarbs;
    private BigDecimal totalFat;
    private BigDecimal totalFiber;
    private Integer totalWaterMl;
    private Integer waterTargetMl;
    private Integer mealCount;
    private List<MealLogDto> meals;
}
