package com.manacommunity.api.hrm.dto;

import com.manacommunity.api.hrm.enums.LeaveSession;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ApplyLeaveRequest {

    @NotNull
    private Long leaveTypeId;

    @NotNull
    private LocalDate fromDate;

    @NotNull
    private LocalDate toDate;

    @NotNull
    private Double totalDays;

    private String reason;

    private Boolean halfDay = false;

    private LeaveSession leaveSession;
}
