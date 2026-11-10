package com.manacommunity.api.fresh.processing.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class RecipeDto {
    private Long id;
    private Long vendorId;
    private String recipeCode;
    private String recipeName;
    private Long outputProductId;
    private String outputProductName;
    private String targetOutputUnit;
    private BigDecimal expectedYieldPercent;
    private BigDecimal storageTempMinCelsius;
    private BigDecimal storageTempMaxCelsius;
    private Integer shelfLifeHours;
    private String instructions;
}