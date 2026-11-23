package com.manacommunity.api.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Platform business and revenue measures, kept separate from uptime and community operations. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessRevenueSummary {
    private Transactions transactions;
    private Revenue revenue;
    private Business business;
    private List<Module> modules;
    private CostCalibration costCalibration;
    private List<String> unavailableMetrics;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Transactions {
        private long paymentsStarted;
        private long paymentsSucceeded;
        private long paymentsFailed;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Revenue {
        private BigDecimal gmv;
        private BigDecimal commission;
        /** Null until platform community subscriptions have a billing source. */
        private BigDecimal subscriptionMrr;
        private Long subscribers;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Business {
        private Long activePayingCommunities;
        private long activeVendors;
        private long completedOrders;
        private long completedBookings;
        private long refundCount;
        private BigDecimal refundedAmount;
        private long repeatBuyers;
        private long uniqueBuyers;
        private Double averageSettlementDelayDays;
        private Long overdueSettlements;
        private Integer settlementSlaDays;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Module {
        private String module;
        private long activeUsers;
        private long orderCount;
        private BigDecimal grossPaymentVolume;
        private BigDecimal estimatedDirectCost;
        private BigDecimal actualCommissionRevenue;
        private BigDecimal actualDirectCost;
        private String marginSource;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CostCalibration {
        private double gatewayMdrRate;
        private double perTxnFixedCost;
        private double moduleHostingAllocation;
    }
}
