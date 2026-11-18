package com.manacommunity.api.payments.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ModuleBreakdown {
    private String referenceType;
    private long count;
    private BigDecimal totalAmount;
}
