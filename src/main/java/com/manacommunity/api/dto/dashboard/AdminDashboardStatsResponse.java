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
    private List<RecentActivityItem> recentActivities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityItem {
        private String title;
        private String timestamp;
        private String type; // e.g. "USER_REGISTERED", "KYC_SUBMITTED", "ROLE_UPDATED"
    }
}
