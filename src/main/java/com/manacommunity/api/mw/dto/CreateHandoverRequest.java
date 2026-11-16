package com.manacommunity.api.mw.dto;

import lombok.Data;

@Data
public class CreateHandoverRequest {
    private Long handoverFromEmployeeId;
    private Long handoverToEmployeeId;
    private Long shiftAssignmentId;
    private String location;
    private String notes;
    private String pendingTasks;
    private String equipmentStatus;
    private Boolean incidentFlag;
    private String incidentDesc;
}
