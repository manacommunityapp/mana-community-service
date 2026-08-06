package com.manacommunity.api.booking.dto;

import com.manacommunity.api.booking.entity.enums.WorkflowStepType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApprovalWorkflowResponse {

    private Long id;
    private Long resourceId;
    private Long categoryId;
    private String workflowName;
    private Integer stepOrder;
    private WorkflowStepType stepType;
    private String stepName;
    private Boolean isRequired;
    private String approverRole;
    private Integer timeoutHours;
    private Boolean isActive;
}
