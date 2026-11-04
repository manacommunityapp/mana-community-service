package com.manacommunity.api.fresh.dispatch.dto;

import com.manacommunity.api.fresh.dispatch.model.SettlementStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data @Builder
public class VendorSettlementDto {
    private Long id;
    private String settlementReference;
    private Long vendorId;
    private String vendorBusinessName;
    private LocalDate settlementPeriodStart;
    private LocalDate settlementPeriodEnd;
    private BigDecimal grossSalesAmount;
    private BigDecimal manaCommissionAmount;
    private BigDecimal deliveryFeesCollected;
    private BigDecimal gatewayFeesDeducted;
    private BigDecimal netPayableAmount;
    private SettlementStatus status;
    private String payoutUtrReference;
    private Instant processedAt;
    private Instant createdAt;
}