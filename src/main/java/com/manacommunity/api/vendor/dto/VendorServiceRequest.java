package com.manacommunity.api.vendor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class VendorServiceRequest {
    @NotBlank
    private String name;
    private String description;
    @NotNull
    private Long categoryId;
    @NotNull
    private BigDecimal basePrice;
    private String priceUnit;
    private Integer durationMinutes;
    private BigDecimal taxPercent;
    private Boolean isAvailable;
    private Integer maxBookingsPerDay;
    private List<String> imageUrls;
    private List<PricingTierRequest> pricingTiers;
    private List<ServiceAreaRequest> serviceAreas;
}
