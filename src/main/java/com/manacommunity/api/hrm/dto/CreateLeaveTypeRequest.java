package com.manacommunity.api.hrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateLeaveTypeRequest {

    @NotBlank
    private String leaveTypeName;

    @NotNull
    private Integer defaultBalance;

    private Boolean isPaid;
    private Boolean isCarryForward;
    private Integer maxCarryForward;
    private Integer approvalLevels;
    private Boolean requiresHrApproval;
    private Integer minNoticeDays;
    private Integer maxConsecutiveDays;
    private Integer maxPerYear;
    private Boolean allowNegativeBalance;
}
