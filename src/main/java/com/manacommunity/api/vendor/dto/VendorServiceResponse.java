package com.manacommunity.api.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class VendorServiceResponse {
    private Long id;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    private BigDecimal basePrice;
    private String priceUnit;
    private Integer durationMinutes;
    private BigDecimal taxPercent;
    private String status;
    private Boolean isAvailable;
    private Integer maxBookingsPerDay;
    private VendorRef vendor;
    private List<String> imageUrls;
    private List<PricingTierResponse> pricingTiers;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class VendorRef {
        private Long id;
        private String businessName;
        private String logoUrl;
        private BigDecimal avgRating;
    }
}
