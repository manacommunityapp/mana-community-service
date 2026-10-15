package com.manacommunity.api.wellness.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealItemDto {
    private Long id;
    private String foodName;
    private String foodNameLocal;
    private BigDecimal quantity;
    private String unit;
    private Integer calories;
    private BigDecimal protein;
    private BigDecimal carbs;
    private BigDecimal fat;
    private BigDecimal fiber;
    private Long foodCatalogId;
}
