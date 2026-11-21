package com.manacommunity.api.serviceplatform.interiors.dto;

import com.manacommunity.api.commerce.core.dto.CommerceOrderDto;
import com.manacommunity.api.serviceplatform.dto.response.ServiceOfferingResponse;
import com.manacommunity.api.serviceplatform.dto.response.WorkOrderResponse;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class InteriorProjectDtos {
    private InteriorProjectDtos() {}

    public record DesignerProfile(
            Long id, Long userId, String userName, String providerType, String businessName,
            String bio, String profileImageUrl, String verificationStatus, BigDecimal avgRating,
            Integer totalJobsCompleted, String serviceAreas, String portfolio, String verifiedCredentialLevel,
            LocalDateTime credentialsVerifiedAt,
            List<ServiceOfferingResponse> offerings) {}

    public record BriefRequest(
            @NotBlank @Size(max = 40) String propertyType,
            @NotBlank @Size(max = 20) String homeSize,
            @NotBlank @Size(max = 120) String locality,
            @Size(max = 60) String stylePreference,
            @Size(max = 10) List<@NotBlank @Size(max = 40) String> focusAreas,
            @DecimalMin("0.0") BigDecimal budgetMin,
            @DecimalMin("0.0") BigDecimal budgetMax,
            @NotBlank @Size(max = 4000) String projectScope,
            LocalDate preferredStart,
            @Size(max = 5) List<Long> designerIds) {}

    public record QuoteRequest(
            @NotNull @DecimalMin(value = "1.0") BigDecimal quotedAmount,
            @NotNull @DecimalMin(value = "1.0") Integer estimatedDurationDays,
            @DecimalMin(value = "0.0") BigDecimal materialAllowance,
            @DecimalMin(value = "0.0") BigDecimal laborAmount,
            @DecimalMin(value = "0.0") BigDecimal taxAmount,
            @Size(max = 2000) String notes,
            @Size(max = 100) List<@Valid QuoteLineItemRequest> lineItems) {}

    public record QuoteLineItemRequest(
            @NotBlank @Size(max = 40) String category,
            @NotBlank @Size(max = 240) String description,
            @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
            @NotBlank @Size(max = 24) String unit,
            @NotNull @DecimalMin(value = "0.0") BigDecimal unitPrice,
            @DecimalMin(value = "0.0") BigDecimal marginPercent,
            @DecimalMin(value = "0.0") BigDecimal taxPercent,
            Long catalogProductId,
            @jakarta.validation.constraints.Min(1) Integer procurementQuantity) {}

    public record QuoteLineItemResponse(
            Long id, String category, String description, BigDecimal quantity, String unit,
            BigDecimal unitPrice, BigDecimal marginPercent, BigDecimal taxPercent, BigDecimal lineTotal,
            Integer position, Long catalogProductId, Integer procurementQuantity) {}

    public record ReviewRequest(
            @NotNull @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(5) Integer rating,
            @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(5) Integer qualityRating,
            @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(5) Integer punctualityRating,
            @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(5) Integer behaviorRating,
            @Size(max = 2000) String comment) {}

    public record QuoteResponse(
            Long id, Long projectId, Long providerId, String providerName,
            BigDecimal quotedAmount, Integer estimatedDurationDays,
            BigDecimal materialAllowance, BigDecimal laborAmount, BigDecimal taxAmount,
            String notes, String status, LocalDateTime createdAt, List<QuoteLineItemResponse> lineItems) {}

    public record ReviewResponse(
            Long id, Long workOrderId, Long providerId, String providerName, Long reviewerId,
            String reviewerName, int rating, Integer qualityRating, Integer punctualityRating,
            Integer behaviorRating, String comment, String providerResponse, boolean verifiedResident,
            LocalDateTime createdAt) {}

    public record ProjectAttachmentsRequest(@Size(max = 10) List<@NotBlank @Size(max = 36) String> mediaIds) {}

    public record AttachmentRef(String mediaId, String fileName, String contentType, Long sizeBytes) {}

    public record ProjectResponse(
            Long id, String title, String status, String propertyType, String homeSize,
            String locality, String stylePreference, List<String> focusAreas,
            BigDecimal budgetMin, BigDecimal budgetMax, String projectScope,
            LocalDate preferredStart, LocalDateTime createdAt, List<Long> invitedDesignerIds,
            List<QuoteResponse> quotes, WorkOrderResponse workOrder, ReviewResponse review,
            List<AttachmentRef> attachments, List<String> milestoneApprovals, List<CommerceOrderDto> materialOrders) {}

    public record InspirationRequest(
            Long projectId, @NotBlank @Size(max=2000) String sourceUrl, @Size(max=2000) String imageUrl,
            @Size(max=240) String title, @Size(max=2000) String notes, @Size(max=1000) String tags) {}
    public record InspirationResponse(Long id, Long projectId, String sourceUrl, String imageUrl, String title,
            String notes, String tags, LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record ChangeRequestCreate(
            @NotBlank @Size(max=2000) String description, @Size(max=4000) String scopeDelta,
            BigDecimal priceDelta, LocalDate proposedStartDate, LocalDate proposedEndDate) {}
    public record ChangeDecision(@NotBlank String decision, @Size(max=2000) String note) {}
    public record ChangeRequestResponse(Long id, Long projectId, Long proposedBy, String description,
            String scopeDelta, BigDecimal priceDelta, LocalDate proposedStartDate, LocalDate proposedEndDate,
            String status, String decisionNote, Long decidedBy, LocalDateTime decidedAt, LocalDateTime createdAt) {}

    public record PaymentMilestoneCreate(@NotBlank @Size(max=180) String title, @Size(max=2000) String description,
            @NotNull @DecimalMin("1.00") BigDecimal amount, LocalDate dueDate,
            @Size(max=120) String invoiceReference, @Size(max=2000) String invoiceUrl) {}
    public record PaymentInvoiceLink(@NotBlank @Size(max=120) String invoiceReference, @Size(max=2000) String invoiceUrl) {}
    public record PaymentRelease(@Size(max=120) String payoutReference) {}
    public record PaymentMilestoneResponse(Long id, Long projectId, Long providerId, String title, String description,
            BigDecimal amount, LocalDate dueDate, String status, String paymentGateway, String razorpayOrderId, String razorpayPaymentId, String cashfreeManaOrderId,
            LocalDateTime authorizedAt, LocalDateTime releasedAt, String payoutReference,
            String invoiceReference, String invoiceUrl) {}
    public record PaymentCheckoutResponse(String gateway, String orderId, BigDecimal amount, String currency,
            String paymentSessionId, String appId, String razorpayKeyId) {}
    public record MilestonePaymentAuthorization(@NotBlank String gateway, @NotBlank String orderId, String paymentId) {}

    public record CredentialCreate(@NotBlank @Size(max=80) String credentialType,
            @NotBlank @Size(max=40) String credentialLevel, @Size(max=180) String issuer,
            @Size(max=120) String credentialNumber, @NotNull UUID evidenceMediaId) {}
    public record CredentialReview(@NotBlank String decision, @Size(max=2000) String note) {}
    public record CredentialResponse(Long id, Long providerId, String credentialType, String credentialLevel,
            String issuer, String credentialNumber, UUID evidenceMediaId, String status, Long submittedBy,
            Long reviewedBy, String reviewNote, LocalDateTime reviewedAt, LocalDateTime createdAt) {}
    public record CredentialAuditResponse(Long id, Long actorUserId, String action, String fromStatus,
            String toStatus, String note, LocalDateTime createdAt) {}
}
