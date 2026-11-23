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

    /**
     * Body of {@code POST /api/marketplace/sellers/kyc/submit}.
     *
     * <p>The raw {@code panNumber} was removed: PAN is never needed in the clear by this
     * service, so the client now sends a digest plus a display fragment, exactly as resident
     * KYC does. Client-side normalisation, which the server must mirror or no lookup will
     * ever match:
     * <pre>
     *   strip all whitespace and hyphens  (/[\s-]/g)
     *   upper-case                        (PAN is alphanumeric)
     *   SHA-256
     *   lowercase hex
     * </pre>
     *
     * <p>{@code gstin} and the bank details are still sent in the clear, for different
     * reasons: a GSTIN is a published identifier, and a settlement account number has to
     * reach the bank as digits. See {@code MarketSellerProfile}.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerKycSubmitRequest {
        private String fssaiLicenseNumber;

        /** Published business identifier; stored as sent. */
        private String gstin;

        /**
         * Lowercase hex SHA-256 of the normalised PAN, computed on the device. Validated
         * against {@code ^[0-9a-f]{64}$} and re-protected with the server pepper before
         * storage — see {@code KycIdNumberProtector}.
         */
        private String panNumberHash;

        /** Last 1-4 characters of the PAN. Display only. */
        private String panNumberLast4;

        /**
         * Retired field. Still bound so that an old client sending a raw PAN gets an explicit
         * 400 instead of having the number quietly accepted; it is never read for storage.
         * Dropping it from the DTO would make such a request succeed with an empty hash.
         */
        private String panNumber;

        /**
         * Settlement account number. Still plaintext on the wire — a payout needs the real
         * digits — but encrypted at rest and never echoed back in full.
         */
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

        /**
         * Masked KYC identifiers, populated only on the paths where the caller is the seller
         * themselves or a community admin reviewing the submission — never on the public
         * seller/listing endpoints, which every resident can call.
         *
         * <p>There is deliberately no field for the full PAN or the full bank account number:
         * the PAN is only held as a peppered digest and could not be returned even if someone
         * wanted to, and the account number is decrypted solely to pay the seller.
         */
        private String gstin;
        private String panNumberLast4;
        private String bankAccountLast4;
        private String bankIfscCode;
        private String bankAccountHolderName;
        private Boolean isActive;
        private Boolean isOpen;
        private BigDecimal rating;
        private Integer totalReviews;
        private Integer totalOrdersCompleted;
        private LocalDateTime createdAt;
    }
}
