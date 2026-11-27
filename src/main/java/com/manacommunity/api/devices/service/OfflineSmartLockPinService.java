package com.manacommunity.api.devices.service;

import com.manacommunity.api.devices.entity.DeviceVisitorOtp;
import com.manacommunity.api.devices.entity.SmartDevice;
import com.manacommunity.api.devices.repository.DeviceVisitorOtpRepository;
import com.manacommunity.api.devices.repository.SmartDeviceRepository;
import com.manacommunity.api.exception.ResourceNotFoundException;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;

/**
 * HKDF + Time-Window HMAC-SHA256 Algorithmic PIN Engine for offline Wi-Fi/BLE smart door locks.
 * Quantizes [validFrom, validUntil] into 15-minute buckets so lock firmware and mobile/backend
 * independently derive the exact same time-bound 6-digit PIN without cloud connectivity.
 */
@Service
@RequiredArgsConstructor
public class OfflineSmartLockPinService {

    public static final long BUCKET_SECONDS = 900L; // 15-minute quantization
    public static final int DEFAULT_DURATION_BUCKETS = 8; // 2 hours
    public static final int MAX_DURATION_BUCKETS = 96; // 24 hours
    private static final String HKDF_INFO_PREFIX = "MANALOCK-OFFLINE-PIN-V1:";
    private static final SecureRandom RNG = new SecureRandom();

    private final SmartDeviceRepository deviceRepo;
    private final DeviceVisitorOtpRepository otpRepo;

    @Builder
    public record OfflinePinDescriptor(
            Long otpId,
            Long deviceId,
            String pinCode,
            String visitorName,
            long slotEpochStart,
            int slotDurationBuckets,
            boolean singleUse,
            Instant validFrom,
            Instant validUntil,
            String derivationMode
    ) {}

    @Builder
    public record OfflineVerificationResult(
            boolean valid,
            String reason,
            long matchedEpochStart,
            int matchedDurationBuckets
    ) {}

    @Transactional
    public OfflinePinDescriptor generateTimeBoundPin(
            Long deviceId,
            Long issuedByUserId,
            String visitorName,
            Instant validFrom,
            Instant validUntil,
            boolean singleUse
    ) {
        SmartDevice device = deviceRepo.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("SmartDevice", deviceId));

        if (device.getMasterSecretHex() == null || device.getMasterSecretHex().isBlank()) {
            byte[] secret = new byte[32];
            RNG.nextBytes(secret);
            device.setMasterSecretHex(HexFormat.of().formatHex(secret));
            device = deviceRepo.save(device);
        }

        Instant from = validFrom != null ? validFrom : Instant.now();
        Instant until = (validUntil != null && validUntil.isAfter(from))
                ? validUntil
                : from.plusSeconds(DEFAULT_DURATION_BUCKETS * BUCKET_SECONDS);

        long startEpoch = from.getEpochSecond() / BUCKET_SECONDS;
        long rawBuckets = Math.max(1L, ((until.getEpochSecond() - (startEpoch * BUCKET_SECONDS)) + BUCKET_SECONDS - 1) / BUCKET_SECONDS);
        int durationBuckets = (int) Math.min(MAX_DURATION_BUCKETS, rawBuckets);

        byte[] derivedSubkey = derivePinSubkey(device.getMasterSecretHex(), deviceId);
        String pinCode = computePinForWindow(derivedSubkey, startEpoch, durationBuckets, singleUse);

        Instant quantizedFrom = Instant.ofEpochSecond(startEpoch * BUCKET_SECONDS);
        Instant quantizedUntil = quantizedFrom.plusSeconds(durationBuckets * BUCKET_SECONDS);

        DeviceVisitorOtp saved = otpRepo.save(DeviceVisitorOtp.builder()
                .deviceId(deviceId)
                .communityId(device.getCommunityId())
                .issuedBy(issuedByUserId)
                .otpCode(pinCode)
                .visitorName(visitorName)
                .validFrom(quantizedFrom)
                .validUntil(quantizedUntil)
                .derivationMode("HKDF_TIME_WINDOW")
                .slotEpochStart(startEpoch)
                .slotDurationBuckets(durationBuckets)
                .singleUse(singleUse)
                .revoked(false)
                .build());

        return OfflinePinDescriptor.builder()
                .otpId(saved.getId())
                .deviceId(deviceId)
                .pinCode(pinCode)
                .visitorName(visitorName)
                .slotEpochStart(startEpoch)
                .slotDurationBuckets(durationBuckets)
                .singleUse(singleUse)
                .validFrom(quantizedFrom)
                .validUntil(quantizedUntil)
                .derivationMode("HKDF_TIME_WINDOW")
                .build();
    }

    /**
     * Simulates exact lock-firmware offline verification (zero DB OTP lookup required).
     * Firmware iterates valid candidate slot anchors covering checkTime and burns single-use digests.
     */
    public OfflineVerificationResult verifyOnOfflineLockFirmware(
            String masterSecretHex,
            Long deviceId,
            String candidatePin,
            Instant checkTime,
            Set<String> firmwareSpentDigests
    ) {
        if (masterSecretHex == null || candidatePin == null || !candidatePin.matches("\\d{6}")) {
            return OfflineVerificationResult.builder().valid(false).reason("INVALID_PIN_FORMAT").build();
        }
        Instant now = checkTime != null ? checkTime : Instant.now();
        long nowEpoch = now.getEpochSecond() / BUCKET_SECONDS;
        byte[] derivedSubkey = derivePinSubkey(masterSecretHex, deviceId);

        int[] supportedDurations = {1, 2, 4, 8, 12, 16, 24, 48, 96};
        for (int duration : supportedDurations) {
            for (long startEpoch = nowEpoch - duration + 1; startEpoch <= nowEpoch; startEpoch++) {
                for (boolean singleUse : new boolean[]{true, false}) {
                    String expected = computePinForWindow(derivedSubkey, startEpoch, duration, singleUse);
                    if (constantTimeEquals(expected, candidatePin)) {
                        String spendKey = startEpoch + ":" + duration + ":" + expected;
                        if (singleUse && firmwareSpentDigests != null) {
                            if (firmwareSpentDigests.contains(spendKey)) {
                                return OfflineVerificationResult.builder()
                                        .valid(false)
                                        .reason("SINGLE_USE_PIN_ALREADY_BURNED")
                                        .build();
                            }
                            firmwareSpentDigests.add(spendKey);
                        }
                        return OfflineVerificationResult.builder()
                                .valid(true)
                                .reason(singleUse ? "AUTHORIZED_SINGLE_USE" : "AUTHORIZED_MULTI_ENTRY")
                                .matchedEpochStart(startEpoch)
                                .matchedDurationBuckets(duration)
                                .build();
                    }
                }
            }
        }
        return OfflineVerificationResult.builder().valid(false).reason("PIN_EXPIRED_OR_INVALID").build();
    }

    public byte[] derivePinSubkey(String masterSecretHex, Long deviceId) {
        byte[] masterBytes = HexFormat.of().parseHex(masterSecretHex.trim());
        String info = HKDF_INFO_PREFIX + deviceId;
        // HKDF-Extract + HKDF-Expand (single 32-byte block)
        byte[] prk = hmacSha256("MANALOCK-SALT-V1".getBytes(StandardCharsets.UTF_8), masterBytes);
        byte[] expandInput = (info + "\u0001").getBytes(StandardCharsets.UTF_8);
        return hmacSha256(prk, expandInput);
    }

    public String computePinForWindow(byte[] subkey, long startEpoch, int durationBuckets, boolean singleUse) {
        String descriptor = startEpoch + "|" + durationBuckets + "|" + (singleUse ? "1" : "0");
        byte[] mac = hmacSha256(subkey, descriptor.getBytes(StandardCharsets.UTF_8));
        int offset = mac[mac.length - 1] & 0x0F;
        int binary = ((mac[offset] & 0x7F) << 24)
                | ((mac[offset + 1] & 0xFF) << 16)
                | ((mac[offset + 2] & 0xFF) << 8)
                | (mac[offset + 3] & 0xFF);
        int code = binary % 1_000_000;
        return String.format("%06d", code);
    }

    private byte[] hmacSha256(byte[] key, byte[] message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(message);
        } catch (Exception ex) {
            throw new RuntimeException("HMAC-SHA256 failed", ex);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
