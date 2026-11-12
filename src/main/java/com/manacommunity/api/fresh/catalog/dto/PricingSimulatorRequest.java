package com.manacommunity.api.fresh.catalog.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder
public class PricingSimulatorRequest {
    private Long vendorProductId;
    private BigDecimal vendorCostPrice;
    private BigDecimal manaCommissionPct;
    private BigDecimal deliveryFee;
    private BigDecimal packagingFee;
    private BigDecimal gstRatePct;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}