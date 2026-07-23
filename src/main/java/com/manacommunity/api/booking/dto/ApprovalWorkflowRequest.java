package com.manacommunity.api.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalWorkflowRequest {

    @NotNull
    private Integer stepOrder;

    @NotNull
    private String stepType;

    private Long resourceId;
    private Long categoryId;
    private Long approverRoleId;
    private Long approverId;
    private String description;
    private Boolean isActive;
}
