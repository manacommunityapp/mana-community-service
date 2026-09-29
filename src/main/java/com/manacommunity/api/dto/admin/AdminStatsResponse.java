package com.manacommunity.api.dto.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsResponse {
    private long totalMembers;
    private long pendingApprovals;
    private long activeMembers;
    private long suspendedMembers;
    private long postsToday;
    private long totalPosts;
    private long eventsThisWeek;
    private long pendingReports;
    private long newMembersThisMonth;
}
