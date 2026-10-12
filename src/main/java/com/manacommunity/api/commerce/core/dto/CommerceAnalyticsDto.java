package com.manacommunity.api.commerce.core.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class CommerceAnalyticsDto {
    String periodFrom;
    String periodTo;
    long totalOrders;
    long completedOrders;
    long cancelledOrders;
    double fulfillmentRate;
    BigDecimal totalRevenue;
    BigDecimal averageOrderValue;
    List<TopProductDto> topProducts;

    @Value
    @Builder
    public static class TopProductDto {
        String sku;
        String title;
        long quantitySold;
    }
}
