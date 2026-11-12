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
}
