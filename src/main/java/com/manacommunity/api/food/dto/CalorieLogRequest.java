package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CalorieLogRequest {
    @NotNull
    private LocalDate date;
    private String mealType;
    @NotBlank
    private String itemName;
    private Integer calories;
    private BigDecimal protein;
    private BigDecimal carbs;
    private BigDecimal fat;
    private BigDecimal fiber;
    private BigDecimal quantity;
    private String unit;
    private String source;
}
