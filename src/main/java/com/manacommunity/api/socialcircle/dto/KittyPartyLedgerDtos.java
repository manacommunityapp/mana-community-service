package com.manacommunity.api.socialcircle.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class KittyPartyLedgerDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCycleRequest {
        @NotBlank(message = "Cycle name is required")
        private String cycleName;

        @NotNull(message = "Contribution amount is required")
        @DecimalMin(value = "1.00", message = "Contribution must be at least ₹1.00")
        private BigDecimal contributionAmount;

        private String frequency; // MONTHLY, WEEKLY

        @NotNull(message = "Member count is required")
        @Min(value = 2, message = "Cycle requires at least 2 members")
        private Integer memberCount;

        @NotNull(message = "Start date is required")
        private LocalDate startDate;

        private List<Long> beneficiaryRotationUserIds; // ordered sequence of userIds for each period
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordContributionRequest {
        @NotNull(message = "Period number is required")
        private Integer periodNumber;

        @NotNull(message = "Recorded amount is required")
        private BigDecimal recordedAmount;

        private String externalRef; // UPI UTR or IMPS Ref
        private String receiptUrl;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyContributionRequest {
        private String paymentStatus; // VERIFIED, DEFAULTED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisbursePayoutRequest {
        @NotNull(message = "Period number is required")
        private Integer periodNumber;

        @NotBlank(message = "External payout reference (UTR) is required")
        private String externalPayoutRef;

        private String payoutReceiptUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CycleSummaryResponse {
        private Long id;
        private Long circleId;
        private String cycleName;
        private BigDecimal contributionAmount;
        private String frequency;
        private Integer memberCount;
        private LocalDate startDate;
        private LocalDate endDate;
        private String financialMode;
        private String status;
        private BigDecimal totalExpectedPerPeriod;
        private BigDecimal totalExpectedFullCycle;
        private BigDecimal totalCollectedCurrentPeriod;
        private BigDecimal totalCollectedFullCycle;
        private BigDecimal pendingContributionsAmount;
        private Integer pendingContributionsCount;
        private Integer paidContributionsCount;
        private Integer currentPeriod;
        private Long currentBeneficiaryUserId;
        private String currentBeneficiaryName;
        private Long nextBeneficiaryUserId;
        private String nextBeneficiaryName;
        private Integer previousPayoutsCount;
        private BigDecimal previousPayoutsTotalAmount;
        private Double cycleProgressPercent;
        private List<RotationEntryDto> rotations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RotationEntryDto {
        private Integer periodNumber;
        private Long beneficiaryUserId;
        private String beneficiaryName;
        private String rotationStatus;
        private LocalDateTime disbursedAt;
        private String externalPayoutRef;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContributionEntryDto {
        private Long id;
        private Long cycleId;
        private Long memberUserId;
        private String memberName;
        private Integer periodNumber;
        private BigDecimal expectedAmount;
        private BigDecimal recordedAmount;
        private String paymentStatus;
        private String externalRef;
        private String receiptUrl;
        private String notes;
        private String verifiedByName;
        private LocalDateTime verifiedAt;
        private LocalDateTime updatedAt;
    }
}
