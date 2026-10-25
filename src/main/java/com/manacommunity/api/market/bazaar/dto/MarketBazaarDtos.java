package com.manacommunity.api.market.bazaar.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class MarketBazaarDtos {
    private MarketBazaarDtos() {}

    public record CreateEventRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 3000) String description,
            Long communityId,
            @NotBlank @Size(max = 300) String venue,
            @Size(max = 100) String city,
            @NotNull LocalDateTime startsAt,
            LocalDateTime endsAt,
            @Size(max = 100) String category,
            @Size(max = 1000) String bannerUrl
    ) {}

    public record CreateStallRequest(
            @NotBlank @Size(max = 40) String stallNumber,
            @Size(max = 100) String zone,
            @NotBlank @Size(max = 100) String packageName,
            @Size(max = 1000) String description,
            @Size(max = 100) String size,
            @NotNull @DecimalMin("1.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
            @Size(max = 5) String currency,
            List<@Size(max = 100) String> facilities,
            List<@Size(max = 100) String> allowedCategories,
            @Min(1) @Max(100) Integer floorRow,
            @Min(1) @Max(100) Integer floorColumn
    ) {}

    public record ApplyRequest(
            @NotNull Long stallId,
            @NotBlank @Size(max = 150) String vendorName,
            @NotBlank @Size(max = 150) String businessName,
            @NotBlank @Size(max = 100) String category,
            @NotBlank @Pattern(regexp = "^[+0-9() -]{8,20}$") String phone,
            @NotBlank @Size(max = 2000) String products,
            List<@Size(max = 500) String> complianceDocuments
    ) {}

    public record RejectRequest(@NotBlank @Size(max = 1000) String reason) {}

    public record EventResponse(
            Long id, String title, String description, Long communityId, String communityName,
            String city, String venue, LocalDateTime startsAt, LocalDateTime endsAt,
            String status, String category, String bannerUrl, long vendorCount, long availableStallCount
    ) {}

    public record StallResponse(
            Long id, Long eventId, String stallNumber, String zone, Long packageId,
            String packageName, String description, String size, BigDecimal price,
            String currency, List<String> facilities, List<String> allowedCategories, String status,
            Integer floorRow, Integer floorColumn
    ) {}

    public record ApplicationResponse(
            Long id, Long eventId, String eventTitle, Long stallId, String stallNumber,
            String vendorName, String businessName, String category, String phone, String products,
            List<String> complianceDocuments, String status, LocalDateTime requestedAt,
            BigDecimal amountDue, String currency, Long bookingId, String paymentOrderId,
            String rejectionReason
    ) {}

    public record EventAnalyticsResponse(
            long totalStalls, long availableStalls, long heldStalls, long bookedStalls,
            long submittedApplications, long confirmedBookings, BigDecimal confirmedRevenue
    ) {}
}





