package com.manacommunity.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal pricePerMeal;
    private Integer mealsPerDay;
    private String mealType;
    private Boolean active;
    private LocalDateTime createdAt;
}
