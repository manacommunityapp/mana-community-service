package com.manacommunity.api.vendor.commerce.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VmsVendorCommerceProfileRequest {

    private Boolean upiEnabled;

    @Size(max = 80)
    private String upiId;

    private Boolean deliveryAvailable;

    @Min(1) @Max(100)
    private Integer deliveryRadiusKm;

    @Min(0) @Max(1440)
    private Integer avgDeliveryTimeMinutes;

    @DecimalMin("0.0")
    private BigDecimal minOrderAmount;

    private Boolean pickupAvailable;

    @Size(max = 400)
    private String pickupAddress;

    private Boolean storefrontEnabled;

    @Size(max = 1000)
    private String storefrontDescription;

    @Size(max = 500)
    private String storefrontBannerUrl;

    private Boolean communityVisible;
}
