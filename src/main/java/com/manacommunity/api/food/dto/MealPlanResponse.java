package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MealPlanResponse {
    private Long id;
    private Long userId;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String goal;
    private Integer dailyCalories;
    private BigDecimal dailyProtein;
    private Long nutritionistId;
    private String nutritionistName;
    private String status;
    private List<MealPlanItemResponse> items;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class MealPlanItemResponse {
        private Long id;
        private String dayOfWeek;
        private String mealType;
        private Long recipeId;
        private String recipeName;
        private String itemName;
        private Integer calories;
        private BigDecimal protein;
        private BigDecimal carbs;
        private BigDecimal fat;
        private String portionSize;
    }
}
