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
public class WellnessPlanDto {
    private String planType;          // BASIC | PREMIUM
    private String displayName;
    private String description;
    private BigDecimal pricePerMonth;
    private BigDecimal pricePerDelivery;
    private Integer deliveriesPerMonth; // 26 (Mon-Sat)
    private List<String> includedFeatures;
    private List<String> boxContentsSummary;
}
