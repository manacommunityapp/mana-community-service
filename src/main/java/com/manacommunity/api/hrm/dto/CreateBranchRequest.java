package com.manacommunity.api.hrm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBranchRequest {

    @NotBlank
    private String branchName;

    private String branchCode;
    private String address;
    private String city;
    private String state;
    private String pincode;
}
