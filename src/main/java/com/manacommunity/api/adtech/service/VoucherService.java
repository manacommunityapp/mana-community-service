package com.manacommunity.api.adtech.service;

import com.manacommunity.api.adtech.dto.VoucherDtos.*;
import com.manacommunity.api.adtech.entity.SampleVoucherEntity;
import com.manacommunity.api.adtech.entity.VoucherClaimEntity;
import com.manacommunity.api.adtech.entity.VoucherRedemptionEntity;
import com.manacommunity.api.adtech.repository.SampleVoucherRepository;
import com.manacommunity.api.adtech.repository.VoucherClaimRepository;
import com.manacommunity.api.adtech.repository.VoucherRedemptionRepository;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final SampleVoucherRepository voucherRepository;
    private final VoucherClaimRepository claimRepository;
    private final VoucherRedemptionRepository redemptionRepository;
    private final VoucherCryptoService cryptoService;

    @Transactional(readOnly = true)
    public List<SampleVoucherDto> listAvailableVouchers(String societyId, AppUser user) {
        String uid = String.valueOf(user.getId());
        Map<String, VoucherClaimEntity> claimsByCid = claimRepository.findByUidOrderByIssuedAtDesc(uid)
                .stream()
                .collect(Collectors.toMap(VoucherClaimEntity::getCid, Function.identity(), (a, b) -> a));

        return voucherRepository.findBySocietyIdAndDeletedFalseOrderByValidUntilAsc(societyId)
                .stream()
                .map(v -> toDto(v, claimsByCid.get(v.getCampaignId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public SampleVoucherDto getVoucher(String voucherId, AppUser user) {
        SampleVoucherEntity voucher = voucherRepository.findByIdAndDeletedFalse(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("SampleVoucher", "id", voucherId));
        VoucherClaimEntity claim = claimRepository.findByCidAndUid(voucher.getCampaignId(), String.valueOf(user.getId()))
                .orElse(null);
        return toDto(voucher, claim);
    }

    @Transactional
    public SampleVoucherDto claimVoucher(String voucherId, AppUser user) {
        SampleVoucherEntity voucher = voucherRepository.findByIdAndDeletedFalse(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("SampleVoucher", "id", voucherId));

        String uid = String.valueOf(user.getId());
        VoucherClaimEntity existing = claimRepository.findByCidAndUid(voucher.getCampaignId(), uid).orElse(null);
        if (existing != null) {
            return toDto(voucher, existing);
        }

        Instant now = Instant.now();
        if (now.isAfter(voucher.getValidUntil())) {
            throw new IllegalStateException("Voucher campaign has expired");
        }
        if (voucher.getMaxClaims() != null && claimRepository.countByVoucherId(voucherId) >= voucher.getMaxClaims()) {
            throw new IllegalStateException("Voucher claim limit reached");
        }

        String vid = "vch_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String nonce = cryptoService.generateNonce(8);
        String totpSecret = cryptoService.generateRandomSecretBase64Url(20);
        String hmacKey = cryptoService.decryptSecret(voucher.getHmacKeyEncrypted());

        VoucherTokenPayloadDto payload = new VoucherTokenPayloadDto(
                1,
                vid,
                uid,
                voucher.getCampaignId(),
                voucher.getSocietyId(),
                now.getEpochSecond(),
                voucher.getValidUntil().getEpochSecond(),
                nonce
        );

        String payloadB64 = cryptoService.encodePayload(payload);
        String hmacSuffix = cryptoService.signPayload(payloadB64, hmacKey);

        VoucherClaimEntity claim = new VoucherClaimEntity();
        claim.setVid(vid);
        claim.setVoucherId(voucher.getId());
        claim.setCid(voucher.getCampaignId());
        claim.setUid(uid);
        claim.setSid(voucher.getSocietyId());
        claim.setNonce(nonce);
        claim.setHmacSuffix(hmacSuffix);
        claim.setTotpSecretEncrypted(cryptoService.encryptSecret(totpSecret));
        claim.setStatus("CLAIMED");
        claim.setIssuedAt(now);
        claim.setExpiresAt(voucher.getValidUntil());
        claimRepository.save(claim);

        return toDto(voucher, claim);
    }

    @Transactional(readOnly = true)
    public List<SampleVoucherDto> getMyClaimedVouchers(AppUser user) {
        String uid = String.valueOf(user.getId());
        return claimRepository.findByUidOrderByIssuedAtDesc(uid).stream()
                .map(claim -> {
                    SampleVoucherEntity voucher = voucherRepository.findByIdAndDeletedFalse(claim.getVoucherId()).orElse(null);
                    return voucher != null ? toDto(voucher, claim) : null;
                })
                .filter(dto -> dto != null)
                .toList();
    }

    @Transactional
    public KioskScanResultDto verifyQrOnline(VerifyQrRequestDto req) {
        VoucherCryptoService.ParsedQr parsed = cryptoService.parseQrString(req.qrString());
        if (parsed == null || parsed.payload() == null || parsed.payload().vid() == null) {
            return rejected("REJECTED_SIGNATURE", null, "Invalid QR format");
        }

        VoucherTokenPayloadDto payload = parsed.payload();
        VoucherClaimEntity claim = claimRepository.findById(payload.vid()).orElse(null);
        if (claim == null) {
            return rejected("REJECTED_SIGNATURE", payload.vid(), "Unknown voucher claim");
        }

        SampleVoucherEntity voucher = voucherRepository.findByIdAndDeletedFalse(claim.getVoucherId()).orElse(null);
        if (voucher == null) {
            return rejected("REJECTED_CAMPAIGN_ENDED", payload.vid(), "Campaign not found");
        }

        String hmacKey = cryptoService.decryptSecret(voucher.getHmacKeyEncrypted());
        if (!cryptoService.verifyPayloadSignature(parsed.payloadB64Url(), parsed.hmacSuffix(), hmacKey)) {
            return rejected("REJECTED_SIGNATURE", payload.vid(), "Invalid cryptographic signature");
        }

        if (cryptoService.isExpired(payload)) {
            return rejected("REJECTED_EXPIRED", payload.vid(), "Voucher has expired");
        }

        String totpSecret = cryptoService.decryptSecret(claim.getTotpSecretEncrypted());
        if (!cryptoService.verifyTotp(totpSecret, parsed.totpCode(), Instant.now().getEpochSecond())) {
            return rejected("REJECTED_OTP_MISMATCH", payload.vid(), "QR code expired — please show live app screen");
        }

        VoucherRedemptionEntity existingRedemption = redemptionRepository.findById(payload.vid()).orElse(null);
        if (existingRedemption != null) {
            return new KioskScanResultDto(
                    "REJECTED_ALREADY_REDEEMED",
                    payload.vid(),
                    payload.uid(),
                    null,
                    null,
                    voucher.getProductName(),
                    voucher.getCampaignName(),
                    existingRedemption.getRedeemedAt().toString(),
                    existingRedemption.getKioskId(),
                    existingRedemption.getKioskId(),
                    "Already redeemed at kiosk " + existingRedemption.getKioskId()
            );
        }

        Instant now = Instant.now();
        VoucherRedemptionEntity redemption = new VoucherRedemptionEntity();
        redemption.setVid(payload.vid());
        redemption.setUid(payload.uid());
        redemption.setKioskId(req.kioskId() != null ? req.kioskId() : "kiosk-unknown");
        redemption.setRedeemedAt(now);
        redemption.setSyncedAt(now);
        redemption.setConflicted(false);
        redemptionRepository.save(redemption);

        claim.setStatus("REDEEMED");
        claim.setRedeemedAt(now);
        claimRepository.save(claim);

        return new KioskScanResultDto(
                "ACCEPTED",
                payload.vid(),
                payload.uid(),
                null,
                null,
                voucher.getProductName(),
                voucher.getCampaignName(),
                now.toString(),
                redemption.getKioskId(),
                null,
                "Sample dispensed ✓"
        );
    }

    @Transactional
    public RedemptionSyncResultDto syncRedemptions(RedemptionSyncPayloadDto payload) {
        List<String> accepted = new ArrayList<>();
        List<RedemptionConflictDto> conflicts = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (payload.records() == null) {
            return new RedemptionSyncResultDto(accepted, conflicts, errors);
        }

        for (LocalRedemptionRecordDto rec : payload.records()) {
            if (rec.vid() == null || rec.vid().isBlank()) {
                errors.add("Missing vid");
                continue;
            }
            VoucherRedemptionEntity existing = redemptionRepository.findById(rec.vid()).orElse(null);
            if (existing != null) {
                if (!existing.getKioskId().equals(rec.kioskId())) {
                    existing.setConflicted(true);
                    redemptionRepository.save(existing);
                    conflicts.add(new RedemptionConflictDto(
                            rec.vid(),
                            existing.getKioskId(),
                            existing.getRedeemedAt().toString()
                    ));
                } else {
                    accepted.add(rec.vid());
                }
                continue;
            }

            Instant redeemedAt = Instant.ofEpochMilli(rec.redeemedAt());
            VoucherRedemptionEntity created = new VoucherRedemptionEntity();
            created.setVid(rec.vid());
            created.setUid(rec.uid() != null ? rec.uid() : "unknown");
            created.setKioskId(rec.kioskId() != null ? rec.kioskId() : payload.kioskId());
            created.setRedeemedAt(redeemedAt);
            created.setSyncedAt(Instant.now());
            created.setConflicted(false);
            redemptionRepository.save(created);

            claimRepository.findById(rec.vid()).ifPresent(claim -> {
                claim.setStatus("REDEEMED");
                claim.setRedeemedAt(redeemedAt);
                claimRepository.save(claim);
            });

            accepted.add(rec.vid());
        }

        return new RedemptionSyncResultDto(accepted, conflicts, errors);
    }

    private KioskScanResultDto rejected(String status, String vid, String message) {
        return new KioskScanResultDto(status, vid, null, null, null, null, null, null, null, null, message);
    }

    private SampleVoucherDto toDto(SampleVoucherEntity v, VoucherClaimEntity claim) {
        String status = "UNCLAIMED";
        SignedVoucherTokenDto signedToken = null;
        String claimedAt = null;
        String redeemedAt = null;

        if (claim != null) {
            status = claim.getStatus();
            claimedAt = claim.getIssuedAt() != null ? claim.getIssuedAt().toString() : null;
            redeemedAt = claim.getRedeemedAt() != null ? claim.getRedeemedAt().toString() : null;
            VoucherTokenPayloadDto tokenPayload = new VoucherTokenPayloadDto(
                    1,
                    claim.getVid(),
                    claim.getUid(),
                    claim.getCid(),
                    claim.getSid(),
                    claim.getIssuedAt().getEpochSecond(),
                    claim.getExpiresAt().getEpochSecond(),
                    claim.getNonce()
            );
            signedToken = new SignedVoucherTokenDto(
                    tokenPayload,
                    claim.getHmacSuffix(),
                    cryptoService.decryptSecret(claim.getTotpSecretEncrypted())
            );
        } else if (Instant.now().isAfter(v.getValidUntil())) {
            status = "EXPIRED";
        }

        return new SampleVoucherDto(
                v.getId(),
                v.getCampaignId(),
                v.getCampaignName(),
                v.getBrandName(),
                v.getBrandLogoUrl(),
                v.getProductName(),
                v.getProductImageUrl(),
                v.getDescription(),
                v.getValidFrom().toString(),
                v.getValidUntil().toString(),
                v.getValidUntil().toString(),
                v.getSocietyId(),
                v.getKioskLocation(),
                v.getKioskOpenFrom(),
                v.getKioskOpenUntil(),
                status,
                claimedAt,
                redeemedAt,
                signedToken
        );
    }
}
