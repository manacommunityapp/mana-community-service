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
public class UserDashboardStatsResponse {

    private String userName;
    private String communityName;
    private long activeEventsCount;
    private long activeNoticesCount;
    private long myBookingsCount;
    private long myTicketsCount;
    private List<QuickNotice> recentNotices;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickNotice {
        private Long id;
        private String title;
        private String category;
        private String createdAt;
    }
}
