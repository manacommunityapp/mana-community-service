package com.manacommunity.api.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class VendorOwnerResponse {
    private Long id;
    private String name;
    private String designation;
    private String email;
    private String phone;
    private BigDecimal ownershipPercent;
}
