package com.manacommunity.api.communityops.manager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CommunityManagerSnapshot(
        Long communityId,
        Instant asOf,
        MaintenanceStats maintenance,
        FinanceStats finance,
        ResidentStats residents,
        SecurityStats security,
        AmenityStats amenities,
        StaffStats staff,
        UpcomingItems upcoming,
        GovernanceStats governance
) {

    public record MaintenanceStats(
            long openTickets,
            long inProgressTickets,
            long resolvedThisWeek,
            long overdueTickets
    ) {}

    public record FinanceStats(
            long unpaidInvoices,
            long overdueInvoices,
            long paidThisMonth,
            BigDecimal overdueAmount
    ) {}

    public record ResidentStats(
            long totalResidents,
            long activeResidents,
            long kycVerified,
            long kycPending
    ) {}

    public record SecurityStats(
            long activeGuards,
            long todayVisitors,
            long pendingNocRequests,
            long securityAuditEvents
    ) {}

    public record AmenityStats(
            long activeBookings,
            long pendingApprovals
    ) {}

    public record StaffStats(
            long staffPresent,
            long staffOnLeave
    ) {}

    public record UpcomingItems(
            List<EventItem> events,
            long pendingNoc,
            long activeElections
    ) {
        public record EventItem(String title, String date) {}
    }

    public record GovernanceStats(
            long activePolls,
            long openProposals,
            long committeePositionsFilled,
            long committeePositionsTotal
    ) {}
}
