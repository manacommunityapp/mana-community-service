package com.manacommunity.api.vendor.commerce.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class VmsVendorCommerceProfileResponse {

    private Long id;
    private Long vendorId;
    private String vendorName;
    private String commerceStatus;

    // Payment
    private Boolean upiEnabled;
    private String upiId;

    // Delivery
    private Boolean deliveryAvailable;
    private Integer deliveryRadiusKm;
    private Integer avgDeliveryTimeMinutes;
    private BigDecimal minOrderAmount;

    // Pickup
    private Boolean pickupAvailable;
    private String pickupAddress;

    // Storefront
    private Boolean storefrontEnabled;
    private String storefrontDescription;
    private String storefrontBannerUrl;
    private Boolean communityVisible;

    // Verification
    private LocalDateTime verifiedAt;
    private String rejectionReason;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
