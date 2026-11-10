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
    public static class SwapRotationRequest {
        @NotNull(message = "From period is required")
        private Integer fromPeriod;

        @NotNull(message = "To period is required")
        private Integer toPeriod;

        @NotBlank(message = "Reason for swapping rotation is required")
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReplaceMemberRequest {
        @NotNull(message = "Old member user ID is required")
        private Long oldUserId;

        @NotNull(message = "New member user ID is required")
        private Long newUserId;

        @NotBlank(message = "Reason for replacement is required")
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendRemindersRequest {
        private Integer periodNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendRemindersResponse {
        private Integer periodNumber;
        private Integer remindedCount;
        private List<String> remindedMembers;
        private String message;
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
        private Integer overdueContributionsCount;
        private BigDecimal overdueContributionsAmount;
        private Integer currentPeriod;
        private Long currentBeneficiaryUserId;
        private String currentBeneficiaryName;
        private Long nextBeneficiaryUserId;
        private String nextBeneficiaryName;
        private Integer previousPayoutsCount;
        private BigDecimal previousPayoutsTotalAmount;
        private Double cycleProgressPercent;
        private String beneficiaryUpiId;
        private String upiDeeplink;
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
        private String swapReason;
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
        private LocalDate dueDate;
        private Boolean isOverdue;
        private Long daysOverdue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditLogEntryDto {
        private Long id;
        private Long cycleId;
        private Long circleId;
        private String eventType;
        private Long actorId;
        private String actorName;
        private String description;
        private String metadataJson;
        private String prevHash;
        private String currHash;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyStatementResponse {
        private Long cycleId;
        private Long circleId;
        private String circleName;
        private String cycleName;
        private Integer periodNumber;
        private LocalDate periodStartDate;
        private LocalDate periodDueDate;
        private BigDecimal contributionAmount;
        private BigDecimal totalExpected;
        private BigDecimal totalCollected;
        private BigDecimal totalPending;
        private Long beneficiaryUserId;
        private String beneficiaryName;
        private String payoutStatus;
        private String externalPayoutRef;
        private LocalDateTime payoutDisbursedAt;
        private String beneficiaryUpiId;
        private String upiDeeplink;
        private List<StatementLineItemDto> lineItems;
        private String legalDisclaimer;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatementLineItemDto {
        private Long memberUserId;
        private String memberName;
        private BigDecimal expectedAmount;
        private BigDecimal recordedAmount;
        private String paymentStatus;
        private String externalRef;
        private String receiptUrl;
        private String notes;
        private String verifiedByName;
        private LocalDateTime verifiedAt;
        private Boolean isOverdue;
        private Long daysOverdue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateDisputeRequest {
        private Long cycleId;

        @NotBlank(message = "Dispute type is required")
        private String disputeType; // PAYMENT_UNRECOGNIZED, PAYOUT_DELAY, AMOUNT_MISMATCH, RULE_VIOLATION, OTHER

        @NotBlank(message = "Title is required")
        private String title;

        @NotBlank(message = "Description is required")
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResolveDisputeRequest {
        @NotBlank(message = "Status is required (RESOLVED or DISMISSED)")
        private String status; // RESOLVED, DISMISSED

        @NotBlank(message = "Resolution notes are required")
        private String resolutionNotes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisputeResponse {
        private Long id;
        private Long circleId;
        private Long cycleId;
        private Long raisedById;
        private String raisedByName;
        private String disputeType;
        private String title;
        private String description;
        private String status;
        private String resolutionNotes;
        private Long resolvedById;
        private String resolvedByName;
        private LocalDateTime resolvedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
