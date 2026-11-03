package com.manacommunity.api.wellnessbasket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessNutritionDto {
    private Long id;
    private Long productId;
    private String productName;
    private String category;
    private BigDecimal energyKcal;
    private BigDecimal proteinG;
    private BigDecimal carbsG;
    private BigDecimal sugarG;
    private BigDecimal fatG;
    private BigDecimal saturatedFatG;
    private BigDecimal fibreG;
    private BigDecimal sodiumMg;
    private Integer glycemicIndex;
    private BigDecimal perServingG;
    private List<String> goalTags;
    private List<String> allergenFlags;
}
