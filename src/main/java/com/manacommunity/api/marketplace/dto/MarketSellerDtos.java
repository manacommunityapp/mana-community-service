package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketSellerProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MarketSellerDtos {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerRegisterRequest {
        @NotBlank(message = "Business/Shop name is required")
        private String businessName;

        @NotNull(message = "Seller type is required")
        private MarketSellerProfile.SellerType sellerType;

        private String storeDescription;
        private String contactPhone;
        private String contactEmail;
        private String flatNumber;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerKycSubmitRequest {
        private String fssaiLicenseNumber;
        private String gstin;
        private String panNumber;

        @NotBlank(message = "Bank account number is required for settlements")
        private String bankAccountNumber;

        @NotBlank(message = "Bank IFSC code is required")
        private String bankIfscCode;

        @NotBlank(message = "Account holder name is required")
        private String bankAccountHolderName;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KycDecisionRequest {
        @NotNull
        private MarketSellerProfile.KycStatus decision; // VERIFIED or REJECTED
        private String rejectionReason;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerProfileResponse {
        private Long id;
        private Long userId;
        private String sellerName;
        private String businessName;
        private MarketSellerProfile.SellerType sellerType;
        private String storeDescription;
        private String contactPhone;
        private String contactEmail;
        private String flatNumber;
        private MarketSellerProfile.KycStatus kycStatus;
        private String kycRejectionReason;
        private Boolean isActive;
        private Boolean isOpen;
        private BigDecimal rating;
        private Integer totalReviews;
        private Integer totalOrdersCompleted;
        private LocalDateTime createdAt;
    }
}
