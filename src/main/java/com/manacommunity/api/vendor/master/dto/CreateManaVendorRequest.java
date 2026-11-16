package com.manacommunity.api.vendor.master.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateManaVendorRequest {

    @NotNull(message = "communityId is required")
    private Long communityId;

    @NotBlank(message = "name is required")
    private String name;

    private String contactPerson;
    private String email;
    private String phone;
    private String gstin;
    private String pan;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String bankDetails;
    private String categories;
    private Integer rating;
    private String sourceModule;
}
