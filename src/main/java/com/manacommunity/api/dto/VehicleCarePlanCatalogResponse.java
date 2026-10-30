package com.manacommunity.api.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCarePlanCatalogResponse {
    private Long id;
    private String name;
    private String description;
    private String frequencyLabel;
    private int washesPerMonth;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private List<String> vehicleTypes;

    // Mobile UI compatibility helpers
    private int creditsPerCycle;
    private int savingsPercent;
    private String frequency;
    private String emoji;
    private boolean popular;
}

