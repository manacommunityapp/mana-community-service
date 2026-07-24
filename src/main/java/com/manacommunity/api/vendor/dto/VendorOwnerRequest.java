package com.manacommunity.api.vendor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VendorOwnerRequest {
    @NotBlank
    private String name;
    private String designation;
    private String email;
    private String phone;
    private String aadhaarNumber;
    private String panNumber;
    private BigDecimal ownershipPercent;
}
