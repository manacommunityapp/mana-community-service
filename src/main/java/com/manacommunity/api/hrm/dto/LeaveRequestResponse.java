package com.manacommunity.api.hrm.dto;

import com.manacommunity.api.hrm.enums.LeaveApprovalStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class LeaveRequestResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long leaveTypeId;
    private String leaveTypeName;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Double totalDays;
    private String reason;
    private LeaveApprovalStatus approvalStatus;
    private String approverRemarks;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
}
