package com.manacommunity.api.hrm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeaveBalanceResponse {
    private Long leaveTypeId;
    private String leaveTypeName;
    private Double totalBalance;
    private Double usedBalance;
    private Double remainingBalance;
}
