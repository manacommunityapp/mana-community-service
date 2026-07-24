package com.manacommunity.api.food.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class MealPlanRequest {
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String goal;
    private Integer dailyCalories;
    private BigDecimal dailyProtein;
    private Long nutritionistId;
    private List<MealPlanItemRequest> items;
}
