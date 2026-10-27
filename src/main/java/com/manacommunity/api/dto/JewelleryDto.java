package com.manacommunity.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class JewelleryDto {

    // ── Vendor ────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VendorResponse {
        private Long id;
        private String shopName;
        private String ownerName;
        private String phoneNumber;
        private String address;
        private String serviceAreas;
        private String specialties;
        private String imageUrl;
        private BigDecimal rating;
        private Integer reviewCount;
        private String openHours;
        private Boolean isVerified;
        private String distance;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterVendorRequest {
        @NotBlank private String shopName;
        @NotBlank private String ownerName;
        @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$") private String phoneNumber;
        private String email;
        @NotBlank private String address;
        private String serviceAreas;
        private String specialties;
        private String gstNumber;
        private String panNumber;
        private String bisHallmarkLicense;
    }

    // ── Product ───────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProductResponse {
        private Long id;
        private Long vendorId;
        private String vendorName;
        private String sku;
        private String name;
        private String category;
        private String material;
        private String purity;
        private BigDecimal netWeightG;
        private String description;
        private String huid;
        private List<String> imageUrls;
        private String tryOnAssetUrl;
        private BigDecimal goldRatePerGram;
        private BigDecimal makingCharges;
        private BigDecimal stoneCost;
        private BigDecimal totalPrice;
        private BigDecimal gstPercent;
        private Integer stockQuantity;
        private String status;
        private Boolean isWishlisted;
        private List<String> tags;
        private Map<String, Object> stoneDetails;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateProductRequest {
        @NotBlank private String name;
        @NotBlank private String category;
        @NotBlank private String material;
        private String purity;
        private BigDecimal netWeightG;
        private String description;
        private String huid;
        private String tags;
        private BigDecimal goldRatePerGram;
        private BigDecimal makingCharges;
        private BigDecimal stoneCost;
        @NotNull @DecimalMin("0.01") private BigDecimal totalPrice;
        private BigDecimal gstPercent;
        @NotNull @Min(0) private Integer stockQuantity;
        private Map<String, Object> stoneDetails;
    }

    // ── Try-On ────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StartTryOnRequest {
        @NotNull  private Long productId;
        @NotBlank private String mode;
        @NotBlank private String platform;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TryOnSessionResponse {
        private Long id;
        private Long productId;
        private String provider;
        private String mode;
        private String platform;
        private String status;
        private String startedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PhotoJobResponse {
        private Long id;
        private String status;
        private String outputUrl;
        private String createdAt;
        private String completedAt;
        private String errorCode;
    }

    // ── Wishlist ──────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WishlistItemResponse {
        private Long id;
        private Long productId;
        private ProductResponse product;
        private String savedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddToWishlistRequest {
        @NotNull private Long productId;
    }

    // ── Appointment ───────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AppointmentResponse {
        private Long id;
        private Long vendorId;
        private String vendorName;
        private String shopAddress;
        private String date;
        private String time;
        private String purpose;
        private String notes;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateAppointmentRequest {
        @NotNull  private Long vendorId;
        @NotNull  private LocalDate date;
        @NotBlank private String time;
        @NotBlank private String purpose;
        private String notes;
    }
}
