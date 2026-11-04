package com.manacommunity.api.fresh.dispatch.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder
public class CreateSettlementRequest {
    private Long vendorId;
    private LocalDate settlementPeriodStart;
    private LocalDate settlementPeriodEnd;
    private BigDecimal grossSalesAmount;
    private BigDecimal manaCommissionAmount;
    private BigDecimal deliveryFeesCollected;
    private BigDecimal gatewayFeesDeducted;
}