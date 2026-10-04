package com.manacommunity.api.homeservices.dto;

import lombok.Data;

@Data
public class AttendanceSummaryResponse {
    private Long staffId;
    private String staffName;
    private String role;
    private long totalPresent;
    private long totalAbsent;
    private long totalOnLeave;
    private long totalNotMarked;
}
