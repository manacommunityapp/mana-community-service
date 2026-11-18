package com.manacommunity.api.payments.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PaymentAnalyticsResponse {

    private long totalOrders;
    private long totalSuccessful;
    private long totalFailed;
    private double successRate;

    /** Sum of all SUCCESS order amounts. Never float. */
    private BigDecimal totalAmount;

    /** Sum of all refunded amounts. Never float. */
    private BigDecimal totalRefunded;

    private List<GatewayBreakdown> breakdownByGateway;
    private List<ModuleBreakdown>  breakdownByModule;
    private List<DailyTrendPoint>  dailyTrend;
}
