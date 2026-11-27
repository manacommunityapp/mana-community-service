package com.manacommunity.api.fm.ops;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record FmOpsDashboardSnapshot(
    long communityId,
    Instant capturedAt,

    ComplaintQueueMetrics complaintQueue,
    AmenityUtilizationMetrics amenityUtilization,
    DuesCollectionMetrics duesCollection,
    EventPipelineMetrics eventPipeline,
    SecurityLogMetrics securityLog,
    StaffRosterMetrics staffRoster,
    BudgetHealthMetrics budgetHealth
) {

    public record ComplaintQueueMetrics(
        long totalOpen,
        long overdueCount,         // open > 7 days
        long inProgressCount,
        long resolvedThisMonth,
        long avgResolutionDays,
        List<CategoryCount> byCategory
    ) {}

    public record CategoryCount(String category, long count) {}

    public record AmenityUtilizationMetrics(
        long totalBookingsThisMonth,
        double avgUtilizationPct,
        List<AmenityUsage> byAmenity,
        List<HourBucket> peakHours
    ) {}

    public record AmenityUsage(String amenityName, long bookings, double utilizationPct) {}

    public record HourBucket(int hour, long bookings) {}

    public record DuesCollectionMetrics(
        BigDecimal totalOutstanding,
        BigDecimal collectedThisMonth,
        double collectionRatePct,
        long overdueResidentCount,
        long totalResidentCount,
        BigDecimal avgDuesPerResident
    ) {}

    public record EventPipelineMetrics(
        long upcomingThisWeek,
        long upcomingThisMonth,
        long pendingApprovals,
        long registrationsOpen,
        long totalRsvpsThisMonth
    ) {}

    public record SecurityLogMetrics(
        long incidentsThisMonth,
        long unresolvedAlerts,
        long gateAccessToday,
        long suspiciousEntries,
        List<IncidentSummary> recentIncidents
    ) {}

    public record IncidentSummary(String type, String description, Instant occurredAt, String severity) {}

    public record StaffRosterMetrics(
        long totalActive,
        long presentToday,
        long absentToday,
        List<RoleHeadcount> byRole
    ) {}

    public record RoleHeadcount(String role, long total, long presentToday) {}

    public record BudgetHealthMetrics(
        BigDecimal maintenanceFundBalance,
        BigDecimal budgetedMonthlyExpense,
        BigDecimal actualMonthlyExpense,
        double variancePct,
        double fundHealthPct,
        BigDecimal projectedRunwayMonths,
        List<ExpenseCategory> topExpenses
    ) {}

    public record ExpenseCategory(String category, BigDecimal amount, double pctOfTotal) {}
}
