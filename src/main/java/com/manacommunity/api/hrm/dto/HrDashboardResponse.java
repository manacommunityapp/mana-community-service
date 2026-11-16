package com.manacommunity.api.hrm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HrDashboardResponse {
    private long totalEmployees;
    private long activeEmployees;
    private long onLeaveToday;
    private long presentToday;
    private long absentToday;
    private long pendingLeaveApprovals;
    private long newJoinersThisMonth;
    private List<HolidayInfo> upcomingHolidays;

    @Data
    @Builder
    public static class HolidayInfo {
        private String holidayName;
        private String holidayDate;
        private Boolean isOptional;
    }
}
