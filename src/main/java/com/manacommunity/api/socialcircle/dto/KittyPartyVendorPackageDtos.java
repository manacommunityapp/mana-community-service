package com.manacommunity.api.socialcircle.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

public class KittyPartyVendorPackageDtos {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorPackageResponse {
        private Long id;
        private Long communityId;
        private Long vendorId;
        private String category;
        private String packageName;
        private String description;
        private String priceRange;
        private String contactPerson;
        private String contactPhone;
        private String sampleMenu;
        private BigDecimal rating;
        private Boolean isActive;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateVendorPackageRequest {
        @NotBlank(message = "Category is required")
        private String category;

        @NotBlank(message = "Package name is required")
        private String packageName;

        private String description;
        private String priceRange;
        private String contactPerson;
        private String contactPhone;
        private String sampleMenu;
        private BigDecimal rating;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InquireVendorPackageRequest {
        private Long packageId;
        private Long eventId;
        private String preferredDate;
        private Integer estimatedGuests;
        private String notes;
    }
}
