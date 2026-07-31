package com.manacommunity.api.vendor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class VendorRequest {
    @NotBlank
    private String businessName;
    private String tradeName;
    @NotNull
    private Long categoryId;
    private String businessType;
    private String description;
    private String email;
    private String phone;
    private String alternatePhone;
    private String website;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String gstNumber;
    private String panNumber;
    private String logoUrl;
    private String bannerUrl;
    private BigDecimal commissionRate;
    private List<VendorOwnerRequest> owners;
    private List<String> languages;
    private List<VendorSocialMediaRequest> socialMedia;
}
