package com.manacommunity.api.adtech.dto;

import java.util.List;

public final class VoucherDtos {

    private VoucherDtos() {}

    public record VoucherTokenPayloadDto(
            int v,
            String vid,
            String uid,
            String cid,
            String sid,
            long iat,
            long exp,
            String nonce
    ) {}

    public record SignedVoucherTokenDto(
            VoucherTokenPayloadDto payload,
            String hmacSuffix,
            String totpSecret
    ) {}

    public record SampleVoucherDto(
            String id,
            String campaignId,
            String campaignName,
            String brandName,
            String brandLogoUrl,
            String productName,
            String productImageUrl,
            String description,
            String validFrom,
            String validUntil,
            String claimDeadline,
            String societyId,
            String kioskLocation,
            String kioskOpenFrom,
            String kioskOpenUntil,
            String status,
            String claimedAt,
            String redeemedAt,
            SignedVoucherTokenDto signedToken
    ) {}

    public record VerifyQrRequestDto(
            String qrString,
            String kioskId
    ) {}

    public record KioskScanResultDto(
            String status,
            String vid,
            String uid,
            String residentName,
            String unitNumber,
            String productName,
            String campaignName,
            String redeemedAt,
            String kioskId,
            String conflictKioskId,
            String message
    ) {}

    public record LocalRedemptionRecordDto(
            String vid,
            String uid,
            long redeemedAt,
            String kioskId,
            boolean synced,
            boolean conflicted
    ) {}

    public record RedemptionSyncPayloadDto(
            String kioskId,
            List<LocalRedemptionRecordDto> records,
            String syncedAt
    ) {}

    public record RedemptionConflictDto(
            String vid,
            String firstKioskId,
            String firstRedeemedAt
    ) {}

    public record RedemptionSyncResultDto(
            List<String> accepted,
            List<RedemptionConflictDto> conflicts,
            List<String> errors
    ) {}
}
