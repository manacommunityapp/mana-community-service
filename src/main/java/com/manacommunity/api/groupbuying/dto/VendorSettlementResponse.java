package com.manacommunity.api.groupbuying.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorSettlementResponse {
    private Long id;
    private Long dealId;
    private String dealTitle;
    private String vendorId;
    private String vendorName;
    private Integer totalOrders;
    private Integer totalQuantity;
    private BigDecimal grossSalesAmount;
    private BigDecimal platformCommissionRate;
    private BigDecimal platformCommissionAmount;
    private BigDecimal communityReserveRate;
    private BigDecimal communityReserveAmount;
    private BigDecimal tierRefundsTotal;
    private BigDecimal netVendorPayout;
    private String settlementStatus; // PENDING, PROCESSING, SETTLED, DISPUTED
    private String payoutReference;
    private LocalDateTime settledAt;
    private LocalDateTime createdAt;
}
