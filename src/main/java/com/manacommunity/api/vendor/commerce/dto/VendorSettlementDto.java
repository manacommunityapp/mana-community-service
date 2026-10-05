package com.manacommunity.api.vendor.commerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorSettlementDto {
    private String id;
    private String vendorId;
    private String dealId;
    private String dealTitle;
    private BigDecimal grossSales;
    private BigDecimal platformFeePct;
    private BigDecimal platformFeeAmount;
    private BigDecimal taxDeducted;
    private BigDecimal netPayoutAmount;
    private String payoutStatus;
    private String bankAccountLast4;
    private String bankName;
    private String settledAt;
    private String createdAt;
    private Integer orderCount;
}
