package com.manacommunity.api.fresh.catalog.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data @Builder
public class FreshProductPricingDto {
    private Long id;
    private Long vendorProductId;
    private String productName;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private BigDecimal vendorCostPrice;
    private BigDecimal manaCommissionPct;
    private BigDecimal deliveryFee;
    private BigDecimal packagingFee;
    private BigDecimal gstRatePct;
    private BigDecimal residentSellingPrice;
    private Boolean isActive;
    private Boolean adminApproved;
    private Instant adminApprovedAt;
}