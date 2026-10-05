package com.manacommunity.api.homeservices.dto;

import com.manacommunity.api.homeservices.model.DomesticStaff;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DomesticStaffRequest {

    @NotBlank
    private String name;

    private String phone;

    @NotNull
    private DomesticStaff.StaffRole role;

    private String shiftTime;
    private String workingTowers;
    private BigDecimal monthlySalary;
    private Boolean verified;
    private Boolean policeVerified;
    private Boolean aadhaarOnFile;
    private String experience;
    private String photoUrl;
}
