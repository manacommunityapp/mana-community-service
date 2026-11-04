package com.manacommunity.api.fresh.catalog.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder
public class FreshProductNutritionDto {
    private Long id;
    private Long vendorProductId;
    private BigDecimal servingSizeGrams;
    private BigDecimal caloriesKcal;
    private BigDecimal proteinG;
    private BigDecimal carbohydratesG;
    private BigDecimal fatG;
    private BigDecimal saturatedFatG;
    private BigDecimal fibreG;
    private BigDecimal sodiumMg;
    private BigDecimal sugarG;
    private String micronutrients;
    private Boolean dietitianCertified;
    private String dietitianName;
    private String certificationNumber;
    private LocalDate certifiedOn;
    private Boolean fssaiLabelCompliant;
}