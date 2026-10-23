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
public class CommerceCommissionRuleDto {
    private Long id;
    private String ruleName;
    private Long vendorUserId;
    private String vendorName;
    private String category;
    private String channel;
    private BigDecimal feePercent;
    private boolean gstOnFee;
    private BigDecimal tdsPercent;
    private int priority;
    private boolean active;
    private String notes;
    private String createdAt;
}
