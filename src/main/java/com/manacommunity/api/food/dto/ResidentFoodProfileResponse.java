package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ResidentFoodProfileResponse {
    private Long id;
    private Long userId;
    private String dietType;
    private Integer calorieGoal;
    private BigDecimal proteinGoal;
    private BigDecimal weightGoal;
    private String healthGoal;
    private String fitnessGoal;
    private Integer waterIntakeGoal;
    private Integer coffeeLimit;
    private BigDecimal dailyNutritionScore;
    private BigDecimal aiLifestyleScore;
    private BigDecimal bmi;
    private List<AllergyResponse> allergies;
    private List<String> cuisinePreferences;
    private List<MealTimingResponse> mealTimings;
    private Integer favoritesCount;
    private List<GoalResponse> goals;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class AllergyResponse {
        private Long id;
        private String allergyName;
        private String severity;
        private String notes;
    }

    @Data
    @Builder
    public static class MealTimingResponse {
        private Long id;
        private String mealType;
        private String preferredTime;
    }

    @Data
    @Builder
    public static class GoalResponse {
        private Long id;
        private String goalType;
        private String targetValue;
        private String currentValue;
        private String status;
    }
}
