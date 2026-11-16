package com.manacommunity.api.hrm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendanceSummaryResponse {
    private long totalPresent;
    private long totalAbsent;
    private long totalOnLeave;
    private long totalHalfDay;
    private long totalWorkingDays;
}
