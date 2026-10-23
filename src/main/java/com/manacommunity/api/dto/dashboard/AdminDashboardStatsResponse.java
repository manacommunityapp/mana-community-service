package com.manacommunity.api.dto.dashboard;

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
    private long openTicketsCount;
    private long inProgressTicketsCount;
    private long activeVendorsCount;
    private long pendingWorkOrdersCount;
    private long pendingExpensesCount;
    private long totalBookingResourcesCount;
    private long pendingContentReportsCount;
    private long activeEventsCount;
    private long activeNoticesCount;

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
}
