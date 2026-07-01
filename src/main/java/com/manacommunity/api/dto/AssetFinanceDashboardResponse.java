package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AssetFinanceDashboardResponse {
    private InventorySummary inventory;
    private FinanceSummary finance;
    private ProcurementSummary procurement;

    @Data
    @Builder
    public static class InventorySummary {
        private long totalAssets;
        private long available;
        private long borrowed;
        private long maintenance;
        private long lost;
        private long disposed;
    }

    @Data
    @Builder
    public static class FinanceSummary {
        private BigDecimal annualBudget;
        private BigDecimal spent;
        private BigDecimal available;
        private long invoicesPending;
        private long outstandingVendorBills;
        private BigDecimal pendingPayments;
    }

    @Data
    @Builder
    public static class ProcurementSummary {
        private long purchaseRequests;
        private long pendingApproval;
        private long purchaseOrdered;
        private long goodsReceived;
    }
}
