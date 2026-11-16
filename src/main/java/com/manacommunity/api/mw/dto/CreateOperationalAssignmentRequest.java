package com.manacommunity.api.mw.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOperationalAssignmentRequest {

    @NotBlank
    private String sourceModule;

    @NotNull
    private Long sourceRefId;

    @NotNull
    private Long employeeId;

    private String assignmentType;
    private String notes;
    private BigDecimal incentiveAmount;
}
