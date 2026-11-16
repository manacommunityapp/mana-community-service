package com.manacommunity.api.cfbos.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CommunityFinanceSummaryResponse {

    private Long communityId;
    private String period;

    // Collections
    private BigDecimal totalBilled;
    private BigDecimal totalCollected;
    private BigDecimal totalOutstanding;
    private BigDecimal totalOverdue;
    private Integer collectionPercent;

    // Flat counts
    private long totalInvoices;
    private long paidInvoices;
    private long overdueInvoices;
    private long pendingInvoices;

    // Expenses
    private BigDecimal totalExpenses;
    private BigDecimal approvedExpenses;
    private BigDecimal pendingExpenses;

    // Payments
    private long totalPayments;
    private BigDecimal totalPaymentsAmount;

    // Aging buckets
    private List<AgingBucket> agingBuckets;

    @Data
    @Builder
    public static class AgingBucket {
        private String period;
        private BigDecimal amount;
        private long count;
        private int percentage;
    }
}
