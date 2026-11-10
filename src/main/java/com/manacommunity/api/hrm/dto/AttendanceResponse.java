package com.manacommunity.api.hrm.dto;

import com.manacommunity.api.hrm.enums.AttendanceStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class AttendanceResponse {
    private LocalDate date;
    private LocalTime firstCheckIn;
    private LocalTime lastCheckOut;
    private Integer totalWorkMinutes;
    private AttendanceStatus status;
    private String remarks;
}
