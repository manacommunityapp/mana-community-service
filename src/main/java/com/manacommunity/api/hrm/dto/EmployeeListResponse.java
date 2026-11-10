package com.manacommunity.api.hrm.dto;

import com.manacommunity.api.hrm.enums.EmploymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class EmployeeListResponse {
    private Long id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String departmentName;
    private String designationName;
    private EmploymentStatus employmentStatus;
    private LocalDate joiningDate;
}
