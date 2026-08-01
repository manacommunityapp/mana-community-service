package com.manacommunity.api.food.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ResidentFoodProfileRequest {
    private String dietType;
    private Integer calorieGoal;
    private BigDecimal proteinGoal;
    private BigDecimal weightGoal;
    private String healthGoal;
    private String fitnessGoal;
    private Integer waterIntakeGoal;
    private Integer coffeeLimit;
    private List<AllergyRequest> allergies;
    private List<String> cuisinePreferences;
}
