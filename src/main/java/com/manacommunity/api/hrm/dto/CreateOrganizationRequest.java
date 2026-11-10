package com.manacommunity.api.hrm.dto;

import com.manacommunity.api.hrm.enums.HrOrganizationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrganizationRequest {

    @NotBlank
    private String organizationName;

    @NotNull
    private HrOrganizationType organizationType;

    private String gstNumber;
    private String registrationNumber;
    private String contactEmail;
    private String contactPhone;
    private String address;
}
