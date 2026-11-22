package com.manacommunity.api.platform.bizrev;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BusinessRevenueDashboardSnapshot(
    Instant asOf,
    LocalDate periodStart,
    LocalDate periodEnd,

    // 1. Active paying communities
    long activePayingCommunities,
    long totalCommunities,
    double communityActivationRatePct,

    // 2. MRR
    BigDecimal monthlyRecurringRevenue,
    BigDecimal mrrGrowthPct,

    // 3. GMV
    BigDecimal grossMerchandiseValue,
    BigDecimal gmvGrowthPct,

    // 4. Commission revenue
    BigDecimal commissionRevenue,
    double commissionMarginPct,

    // 5. Active vendors
    long activeVendors,
    long newVendorsThisMonth,

    // 6. Completed orders & bookings
    long completedOrders,
    long completedBookings,

    // 7. Payment failure rate
    double paymentFailureRatePct,
    long failedPayments,
    long totalPayments,

    // 8. Refund rate
    double refundRatePct,
    BigDecimal totalRefundedAmount,

    // 9. Repeat purchase rate
    double repeatPurchaseRatePct,
    long repeatBuyers,
    long totalBuyers,

    // 10. Vendor settlement delay
    double avgSettlementDelayDays,
    long overdueSettlements,

    // 11. Contribution margin by module
    List<ModuleMargin> moduleMargins
) {
    public record ModuleMargin(
        String module,
        BigDecimal revenue,
        BigDecimal cost,
        BigDecimal contributionMargin,
        double marginPct
    ) {}
}
