package com.manacommunity.api.commerce.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommerceSettlementDto {
    private Long id;
    private String settlementNumber;
    private String vendorId;
    private Long sellerId;
    private Long communityId;
    private String cycleStartDate;
    private String cycleEndDate;
    private Integer totalOrdersCount;
    private BigDecimal grossAmount;
    private BigDecimal platformFee;
    private BigDecimal deductions;
    private BigDecimal netPayout;
    private String status;
    private String payoutUtr;
    private String payoutDate;
}