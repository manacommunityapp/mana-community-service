package com.manacommunity.api.hrm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateDepartmentRequest {

    @NotBlank
    private String departmentName;

    private String departmentCode;
}
