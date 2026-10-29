package com.manacommunity.api.wellness.dto;

import com.manacommunity.api.wellness.enums.WellnessEnums.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealLogDto {
    private Long id;
    private Long userId;
    private MealType mealType;
    private LocalDate mealDate;
    private LocalTime mealTime;
    private MealSourceType sourceType;
    private String originalInput;
    private String photoUrl;
    private Integer totalCalories;
    private BigDecimal totalProtein;
    private BigDecimal totalCarbs;
    private BigDecimal totalFat;
    private BigDecimal totalFiber;
    private BigDecimal aiConfidence;
    private BigDecimal junkFoodScore;
    private Boolean isJunkFood;
    private MealLogStatus status;
    private String notes;
    private List<MealItemDto> items;
}
