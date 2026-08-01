package com.manacommunity.api.vendor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceAreaRequest {
    @NotBlank
    private String areaName;
    private String pincode;
    private BigDecimal radiusKm;
    private BigDecimal additionalCharge;
}
