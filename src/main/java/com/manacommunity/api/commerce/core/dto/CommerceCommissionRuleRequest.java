package com.manacommunity.api.commerce.core.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CommerceCommissionRuleRequest {
    private String ruleName;
    private Long vendorUserId;
    private String category;
    private String channel;
    private BigDecimal feePercent;
    private Boolean gstOnFee;
    private BigDecimal tdsPercent;
    private Integer priority;
    private Boolean active;
    private String notes;
}
