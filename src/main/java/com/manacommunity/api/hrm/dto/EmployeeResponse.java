package com.manacommunity.api.hrm.dto;

import com.manacommunity.api.hrm.enums.EmploymentStatus;
import com.manacommunity.api.hrm.enums.EmploymentType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class EmployeeResponse {
    private Long id;
    private Long organizationId;
    private String employeeCode;
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
    private Long manaUserId;
    private Long branchId;
    private String branchName;
    private Long departmentId;
    private String departmentName;
    private Long designationId;
    private String designationName;
    private Long reportingManagerId;
    private String reportingManagerName;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
