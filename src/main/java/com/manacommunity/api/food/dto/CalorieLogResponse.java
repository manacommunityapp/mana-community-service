package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CalorieLogResponse {
    private Long id;
    private Long userId;
    private LocalDate date;
    private String mealType;
    private String itemName;
    private Integer calories;
    private BigDecimal protein;
    private BigDecimal carbs;
    private BigDecimal fat;
    private BigDecimal fiber;
    private BigDecimal quantity;
    private String unit;
    private String source;
    private LocalDateTime createdAt;
}
