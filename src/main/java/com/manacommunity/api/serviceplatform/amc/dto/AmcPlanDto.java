package com.manacommunity.api.serviceplatform.amc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmcPlanDto {
    private Long id;
    private Long providerId;
    private String providerName;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private String planType;
    private BigDecimal price;
    private int visitsIncluded;
    private int durationMonths;
    private String terms;
    private boolean active;
}
