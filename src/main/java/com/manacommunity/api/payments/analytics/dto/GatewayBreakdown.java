package com.manacommunity.api.payments.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class GatewayBreakdown {
    private String gateway;
    private long count;
    private BigDecimal totalAmount;
    private double successRate;
}
