package com.manacommunity.api.hrm.dto;

import com.manacommunity.api.hrm.enums.EmploymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateEmployeeRequest {

    private Long branchId;
    private Long departmentId;
    private Long designationId;

    @NotNull
    private Long manaUserId;

    private String employeeCode;

    @NotBlank
    private String firstName;

    private String lastName;
    private String personalEmail;
    private String workEmail;
    private String phone;

    @NotNull
    private EmploymentType employmentType;

    private LocalDate joiningDate;
    private Long reportingManagerId;
    private String emergencyContactName;
    private String emergencyContactPhone;
}
