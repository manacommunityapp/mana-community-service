package com.manacommunity.api.marketplace.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketAdminMetricsDto {

    private BigDecimal totalGmv;
    private int totalOrders;
    private int activeListings;
    private int openDisputes;
    private int pendingReports;
    private int activeGroupOrders;
    private Map<String, Integer> categoryDistribution;
    private Map<String, BigDecimal> monthlyRevenue;
}
