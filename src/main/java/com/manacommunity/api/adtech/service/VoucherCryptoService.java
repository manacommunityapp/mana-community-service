package com.manacommunity.api.adtech.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.adtech.dto.VoucherDtos.VoucherTokenPayloadDto;
import com.manacommunity.api.security.FieldEncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

/**
 * Cryptographic operations for offline-capable Voucher QR verification:
 * 1) Truncated 128-bit HMAC-SHA256 signature on VoucherTokenPayload
 * 2) RFC 6238 HMAC-SHA1 TOTP generation/verification (30s period, ±1 step clock skew)
 * 3) AES-256-GCM storage encryption via FieldEncryptionService
 */
@Service
@RequiredArgsConstructor
public class VoucherCryptoService {

    public static final String QR_PREFIX = "MANA:v1:";
    private static final int TOTP_PERIOD_SECONDS = 30;
    private static final int TOTP_DIGITS = 6;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder B64U_ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64U_DEC = Base64.getUrlDecoder();

    private final FieldEncryptionService encryptionService;
    private final ObjectMapper objectMapper;

    public String generateRandomSecretBase64Url(int numBytes) {
        byte[] bytes = new byte[numBytes];
        SECURE_RANDOM.nextBytes(bytes);
        return B64U_ENC.encodeToString(bytes);
    }

    public String generateNonce(int length) {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public String signPayload(String payloadB64Url, String hmacKeyBase64Url) {
        try {
            byte[] keyBytes = B64U_DEC.decode(hmacKeyBase64Url);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            byte[] fullHmac = mac.doFinal(payloadB64Url.getBytes(StandardCharsets.UTF_8));
            byte[] truncated = Arrays.copyOf(fullHmac, 16);
            return B64U_ENC.encodeToString(truncated);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign voucher payload", ex);
        }
    }

    public boolean verifyPayloadSignature(String payloadB64Url, String candidateHmacSuffix, String hmacKeyBase64Url) {
        if (payloadB64Url == null || candidateHmacSuffix == null || hmacKeyBase64Url == null) {
            return false;
        }
        String expected = signPayload(payloadB64Url, hmacKeyBase64Url);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                candidateHmacSuffix.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateTotp(String secretBase64Url, long unixSeconds, int digits, int period) {
        try {
            byte[] secretBytes = B64U_DEC.decode(secretBase64Url);
            long counter = Math.floorDiv(unixSeconds, period);
            byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA1"));
            byte[] hash = mac.doFinal(counterBytes);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int mod = (int) Math.pow(10, digits);
            int otp = binary % mod;
            return String.format("%0" + digits + "d", otp);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate TOTP", ex);
        }
    }

    public boolean verifyTotp(String secretBase64Url, String candidateOtp, long unixSeconds) {
        if (candidateOtp == null || candidateOtp.length() != TOTP_DIGITS) {
            return false;
        }
        for (int offset : new int[]{-1, 0, 1}) {
            String expected = generateTotp(
                    secretBase64Url,
                    unixSeconds + (long) offset * TOTP_PERIOD_SECONDS,
                    TOTP_DIGITS,
                    TOTP_PERIOD_SECONDS
            );
            if (MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    candidateOtp.getBytes(StandardCharsets.UTF_8))) {
                return true;
            }
        }
        return false;
    }

    public ParsedQr parseQrString(String qrString) {
        if (qrString == null || !qrString.startsWith(QR_PREFIX)) {
            return null;
        }
        String body = qrString.substring(QR_PREFIX.length());
        String[] parts = body.split("\\.", -1);
        if (parts.length != 3) {
            return null;
        }
        try {
            byte[] decoded = B64U_DEC.decode(parts[0]);
            VoucherTokenPayloadDto payload = objectMapper.readValue(decoded, VoucherTokenPayloadDto.class);
            return new ParsedQr(parts[0], parts[1], parts[2], payload);
        } catch (Exception ex) {
            return null;
        }
    }

    public String encodePayload(VoucherTokenPayloadDto payload) {
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(payload);
            return B64U_ENC.encodeToString(jsonBytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize voucher payload", ex);
        }
    }

    public boolean isExpired(VoucherTokenPayloadDto payload) {
        return Instant.now().getEpochSecond() > payload.exp();
    }

    public String encryptSecret(String plaintext) {
        return encryptionService.encrypt(plaintext);
    }

    public String decryptSecret(String ciphertext) {
        return encryptionService.decrypt(ciphertext);
    }

    public record ParsedQr(
            String payloadB64Url,
            String hmacSuffix,
            String totpCode,
            VoucherTokenPayloadDto payload
    ) {}
}
