package com.manacommunity.api.vendor.commerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorAnalyticsDto {
    private BigDecimal totalGrossRevenue;
    private Integer totalOrdersFulfilled;
    private BigDecimal averageOrderValue;
    private BigDecimal sellThroughRate;
    private BigDecimal repeatBuyerPct;
    private BigDecimal onTimeDeliveryRate;
    private BigDecimal disputeResolutionRate;
    private List<TopProductItem> topProducts;
    private List<MonthlyRevenueItem> monthlyRevenueChart;
    private List<CategoryDistributionItem> categoryDistribution;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProductItem {
        private String name;
        private Integer unitsSold;
        private BigDecimal revenue;
        private BigDecimal marginPct;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenueItem {
        private String month;
        private BigDecimal revenue;
        private Integer orders;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDistributionItem {
        private String category;
        private Integer count;
        private BigDecimal percentage;
    }
}
