package com.manacommunity.api.vendor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class VendorRegistrationRequest {
    @NotBlank
    private String businessName;
    @NotNull
    private Long categoryId;
    private String businessType;
    @NotBlank
    private String contactName;
    @NotBlank
    private String contactEmail;
    @NotBlank
    private String contactPhone;
    private String description;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String gstNumber;
    private String panNumber;
    private List<String> documentUrls;
}
