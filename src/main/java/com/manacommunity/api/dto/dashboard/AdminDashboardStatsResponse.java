package com.manacommunity.api.dto.dashboard;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsResponse {

    private long totalUsers;
    private long pendingKycCount;
    private long verifiedUsersCount;
    private long totalRolesCount;
    private long totalCommunitiesCount;

    private long activeVisitorsCount;
    private long expectedVisitorsTodayCount;
    private Double facilityUtilizationPercent;
    private long openTicketsCount;
    private long inProgressTicketsCount;
    private long activeVendorsCount;
    private long pendingWorkOrdersCount;
    private long pendingExpensesCount;
    private long totalBookingResourcesCount;
    private long pendingContentReportsCount;
    private long activeEventsCount;
    private long activeNoticesCount;

    private long ownerCount;
    private long tenantCount;
    private long overdueInvoicesCount;
    private BigDecimal outstandingDuesAmount;
    private BigDecimal totalExpensesAmount;
    private BigDecimal pendingExpensesAmount;
    private long breachedSlaTicketsCount;
    private long facilityBookingsTodayCount;
    private long contractRenewalsDueNext30DaysCount;
    private long eventAttendanceCount;
    private long engagedResidentsLast30DaysCount;
    private List<BudgetSummary> budgetSummaries;

    // Fields consumed by the community admin dashboard.
    private long residentCount;
    private long outstandingInvoiceCount;
    private BigDecimal maintenanceCollectedThisMonthAmount;
    private long slaBreachedTicketsCount;
    private long monthlyFacilityBookingsCount;
    private long activeVendorContractsCount;
    private long contractsExpiringWithin30DaysCount;
    private BigDecimal monthlyApprovedExpenseAmount;
    private BigDecimal currentFinancialYearBudgetAmount;
    private BigDecimal currentFinancialYearSpentAmount;
    private long communityPostsLast30DaysCount;

    private List<RecentActivityItem> recentActivities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityItem {
        private String title;
        private String timestamp;
        private String type;
        private String module;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetSummary {
        private String financialYear;
        private BigDecimal allocatedAmount;
        private BigDecimal spentAmount;
    }
}
