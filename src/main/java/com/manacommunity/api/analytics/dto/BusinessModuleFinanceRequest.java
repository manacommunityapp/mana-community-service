package com.manacommunity.api.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BusinessModuleFinanceRequest {
    @NotBlank private String moduleCode;
    @NotNull private LocalDate reportDate;
    @NotNull private BigDecimal commissionRevenue;
    @NotNull private BigDecimal directCost;
}
