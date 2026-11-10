package com.manacommunity.api.hrm.dto;

import com.manacommunity.api.hrm.enums.EmploymentStatus;
import com.manacommunity.api.hrm.enums.EmploymentType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateEmployeeRequest {
    private Long branchId;
    private Long departmentId;
    private Long designationId;
    private String firstName;
    private String lastName;
    private String personalEmail;
    private String workEmail;
    private String phone;
    private EmploymentType employmentType;
    private EmploymentStatus employmentStatus;
    private LocalDate joiningDate;
    private LocalDate confirmationDate;
    private LocalDate exitDate;
    private Long reportingManagerId;
    private String emergencyContactName;
    private String emergencyContactPhone;

    @NotNull(message = "Version is required for optimistic locking")
    private Long version;
}
