package com.manacommunity.api.wellnessbasket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessMenuDayDto {
    private LocalDate deliveryDate;
    private String dayOfWeekName; // Monday, Tuesday, etc.
    private Short dayOfWeek;      // 1-6
    private String planType;
    private List<WellnessNutritionDto> products;
    private List<WellnessNutritionDto> alternativeProducts;
    private BigDecimal totalEnergyKcal;
    private BigDecimal totalProteinG;
    private BigDecimal totalCarbsG;
    private BigDecimal totalFatG;
    private BigDecimal totalFibreG;
    private String dietitianNotes;
    private boolean isSwapped;
    private Long activeSwapId;
    private String activeSwapStatus;
}
