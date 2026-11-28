package com.manacommunity.api.clubhouse.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ClubhouseDtos {

    private ClubhouseDtos() {}

    public record FlatTabAccountDto(
            UUID accountId,
            long societyId,
            long flatId,
            String unitCode,
            long creditLimitPaise,
            long unbilledTabPaise,
            long activeHoldsPaise,
            long availableCreditPaise,
            long offlineFloorCapPaise,
            String billingPreference,
            boolean overdueFrozen,
            int pinFailedAttempts,
            Instant pinLockedUntil
    ) {}

    public record PosOrderRequest(
            long societyId,
            long flatId,
            String unitCode,
            String terminalId,
            String outletName,
            String clientTxUuid,
            String itemsSummary,
            long subtotalPaise,
            long gstPaise,
            String settlementMode,
            String authMethod,
            String pin,
            String offlineTotpQr
    ) {}

    public record OfflineOrderSyncItem(
            String clientTxUuid,
            long flatId,
            String unitCode,
            String terminalId,
            String outletName,
            String itemsSummary,
            long subtotalPaise,
            long gstPaise,
            String authMethod,
            Instant orderedAt
    ) {}

    public record OfflineSyncBatchRequest(
            long societyId,
            List<OfflineOrderSyncItem> items
    ) {}

    public record PosTabChallengeDto(
            UUID challengeId,
            long societyId,
            long flatId,
            String unitCode,
            String terminalId,
            String outletName,
            long subtotalPaise,
            long gstPaise,
            long totalPaise,
            String itemsSummary,
            String authMethod,
            String status,
            String approvedBy,
            Instant expiresAt,
            Instant createdAt
    ) {}

    public record PosOrderDto(
            UUID orderId,
            long societyId,
            long flatId,
            String unitCode,
            String terminalId,
            String outletName,
            String clientTxUuid,
            String itemsSummary,
            long subtotalPaise,
            long gstPaise,
            long totalPaise,
            String settlementMode,
            String authMethod,
            String status,
            boolean provisionalOffline,
            boolean offlineOverrun,
            String hashChainDigest,
            UUID batchId,
            Instant orderedAt
    ) {}

    public record PosAuthorizationResponse(
            String outcome,
            PosTabChallengeDto challenge,
            PosOrderDto order,
            FlatTabAccountDto account
    ) {}

    public record ApproveChallengeRequest(
            String approvedBy,
            boolean approve
    ) {}

    public record OfflineSyncResultDto(
            int totalSubmitted,
            int syncedCount,
            int deduplicatedCount,
            int overrunFreezeCount,
            List<PosOrderDto> orders
    ) {}

    public record ReconcileBatchRequest(
            long societyId,
            String terminalId,
            String businessDate
    ) {}

    public record PosDailyBatchDto(
            UUID batchId,
            long societyId,
            String terminalId,
            String businessDate,
            int batchSeq,
            int orderCount,
            long grossPaise,
            long gstPaise,
            long flatTabPaise,
            long walletPaise,
            int offlineOverrunCount,
            String hashChainRoot,
            String erpStatus,
            String erpVoucherRef,
            Instant closedAt
    ) {}

    public record OfflineSnapshotEntry(
            long flatId,
            String unitCode,
            long availableCreditPaise,
            long offlineFloorCapPaise,
            boolean overdueFrozen,
            String offlinePinVerifier
    ) {}

    public record OfflineTerminalSnapshotDto(
            long societyId,
            String generatedAt,
            String signatureHex,
            List<OfflineSnapshotEntry> flats
    ) {}

    public record GuestPassQuoteRequest(
            long societyId,
            String facilityCode,
            long flatId,
            String visitDate,
            int guestCount
    ) {}

    public record GuestSlotBreakdownDto(
            int guestIndex,
            String pricingTier,
            long unitPricePaise,
            long gstPaise,
            long totalPaise
    ) {}

    public record GuestPassQuoteResponse(
            long societyId,
            String facilityCode,
            long flatId,
            String billingMonth,
            String visitDate,
            String dayCategory,
            int freeLimit,
            int freeConsumed,
            int freeReserved,
            int freeRemainingBefore,
            int freeSlotsAllocated,
            int paidSlotsRequired,
            long paidUnitPricePaise,
            long totalPayablePaise,
            List<GuestSlotBreakdownDto> breakdown
    ) {}

    public record IssueGuestPassesRequest(
            long societyId,
            String facilityCode,
            long flatId,
            String unitCode,
            String visitDate,
            List<String> guestNames,
            String guestPhone,
            String paymentMode
    ) {}

    public record GuestAccessPassDto(
            UUID passId,
            long societyId,
            String facilityCode,
            long flatId,
            String unitCode,
            String billingMonth,
            String visitDate,
            String guestName,
            String guestPhone,
            String pricingTier,
            long unitPricePaise,
            long gstPaise,
            long totalPaise,
            String jti,
            String qrToken,
            String status,
            int entryCount,
            int maxEntries,
            Instant validFrom,
            Instant validUntil,
            Instant consumedAt
    ) {}

    public record IssueGuestPassesResponse(
            GuestPassQuoteResponse quote,
            List<GuestAccessPassDto> passes
    ) {}

    public record TurnstileValidateRequest(
            long societyId,
            String facilityCode,
            String turnstileId,
            String qrToken
    ) {}

    public record TurnstileValidateResponse(
            boolean accessGranted,
            String reasonCode,
            String message,
            UUID passId,
            String jti,
            String unitCode,
            String guestName,
            String pricingTier,
            int poolOccupancy,
            int poolCapacity
    ) {}
}
