package com.manacommunity.api.analytics.dto;

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
public class PilotKpisResponse {

    private long activeResidents;
    private long signups;
    private long activatedResidents;
    private List<FunnelStageDto> funnel;
    private TransactionsKpiDto transactions;
    private List<VendorKpiDto> vendors;
    private SupportKpiDto support;
    private RevenueKpiDto revenue;
    private List<ModuleKpiDto> modules;
    private CostCalibrationDto costCalibration;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostCalibrationDto {
        private double gatewayMdrRate;
        private double perTxnFixedCost;
        private double moduleHostingAllocation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunnelStageDto {
        private String stage;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionsKpiDto {
        private long bookingsCreated;
        private long paymentsStarted;
        private long paymentsSucceeded;
        private long paymentsFailed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorKpiDto {
        private Long vendorId;
        private String name;
        private long orders;
        private double acceptanceRate;
        private double onTimeRate;
        private double cancellationRate;
        private double disputeRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupportKpiDto {
        private long tickets;
        private Double avgFirstResponseHours;
        private Double avgResolutionHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueKpiDto {
        private BigDecimal gmv;
        private BigDecimal commission;
        private BigDecimal subscriptionMrr;
        private long subscribers;
        private long churnedSubscribers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModuleKpiDto {
        private String module;
        private long activeUsers;
        private long orderCount;
        private BigDecimal revenue;
        private BigDecimal cost;
    }
}
