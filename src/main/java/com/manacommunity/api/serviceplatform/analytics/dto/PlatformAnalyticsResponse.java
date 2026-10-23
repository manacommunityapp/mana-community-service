package com.manacommunity.api.serviceplatform.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformAnalyticsResponse {
    private long totalRequests;
    private long completedWorkOrders;
    private long pendingWorkOrders;
    private long cancelledRequests;
    private double averageFulfillmentTimeHours;
    private double overallSatisfactionRating;
    private BigDecimal totalRevenueGenerated;
    private Map<String, Long> requestsByCategory;
    private Map<String, Long> requestsByStatus;
}
