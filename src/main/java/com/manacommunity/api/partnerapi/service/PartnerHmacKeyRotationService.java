package com.manacommunity.api.partnerapi.service;

import com.manacommunity.api.partnerapi.entity.PartnerWebhookKey;
import com.manacommunity.api.partnerapi.entity.WebhookNonceJournal;
import com.manacommunity.api.partnerapi.repository.PartnerWebhookKeyRepository;
import com.manacommunity.api.partnerapi.repository.WebhookNonceJournalRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dual-Key Zero-Downtime HMAC-SHA256 Signing & Nonce Replay Protection Service.
 * Prevents compromised hardware controller secrets from spoofing gate openings across updates.
 */
@Service
@RequiredArgsConstructor
public class PartnerHmacKeyRotationService {

    public static final long MAX_TIMESTAMP_DRIFT_SECONDS = 300L;
    private static final SecureRandom RNG = new SecureRandom();

    private final PartnerWebhookKeyRepository keyRepo;
    private final WebhookNonceJournalRepository nonceRepo;

    @Builder
    public record VerificationResult(
            boolean valid,
            String statusCode, // VERIFIED, TIMESTAMP_EXPIRED, REPLAY_DETECTED, KEY_REVOKED, INVALID_SIGNATURE
            String partnerId,
            String keyId,
            String keyStatus,
            String reason
    ) {}

    @Transactional
    public PartnerWebhookKey issueOrRotateKey(String partnerId, int graceHours) {
        String normPartner = partnerId.trim().toUpperCase(Locale.ROOT);
        Instant now = Instant.now();
        int effectiveGraceHours = graceHours > 0 ? graceHours : 168; // Default 7 days grace

        List<PartnerWebhookKey> existing = keyRepo.findByPartnerIdOrderByCreatedAtDesc(normPartner);
        int nextVersion = existing.size() + 1;
        for (PartnerWebhookKey oldKey : existing) {
            if ("ACTIVE".equals(oldKey.getStatus())) {
                oldKey.setStatus("ROTATING_GRACE");
                oldKey.setGraceUntil(now.plusSeconds(effectiveGraceHours * 3600L));
                keyRepo.save(oldKey);
            }
        }

        byte[] rawSecret = new byte[32];
        RNG.nextBytes(rawSecret);
        String keyId = normPartner.toLowerCase(Locale.ROOT) + "-k" + nextVersion;

        return keyRepo.save(PartnerWebhookKey.builder()
                .partnerId(normPartner)
                .keyId(keyId)
                .secretHex(HexFormat.of().formatHex(rawSecret))
                .status("ACTIVE")
                .build());
    }

    @Transactional
    public PartnerWebhookKey revokeKeyImmediately(String keyId, String reason) {
        PartnerWebhookKey key = keyRepo.findByKeyId(keyId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Key ID not found: " + keyId));
        key.setStatus("REVOKED");
        key.setRevokedReason(reason != null ? reason : "COMPROMISED_FIRMWARE_QUARANTINE");
        return keyRepo.save(key);
    }

    @Transactional
    public VerificationResult verifyCanonicalRequest(
            String partnerId,
            String keyId,
            String httpMethod,
            String path,
            String timestampHeader,
            String nonce,
            String rawBody,
            String signatureHeader,
            Instant nowOverride
    ) {
        Instant now = nowOverride != null ? nowOverride : Instant.now();
        String normPartner = partnerId != null ? partnerId.trim().toUpperCase(Locale.ROOT) : "";

        if (timestampHeader == null || timestampHeader.isBlank() || signatureHeader == null || signatureHeader.isBlank()) {
            return VerificationResult.builder().valid(false).statusCode("INVALID_SIGNATURE")
                    .partnerId(normPartner).keyId(keyId).reason("Missing X-Timestamp or X-Signature").build();
        }
        if (nonce == null || nonce.isBlank()) {
            return VerificationResult.builder().valid(false).statusCode("INVALID_SIGNATURE")
                    .partnerId(normPartner).keyId(keyId).reason("Missing X-Nonce replay token").build();
        }

        long ts;
        try {
            ts = Long.parseLong(timestampHeader.trim());
        } catch (NumberFormatException ex) {
            return VerificationResult.builder().valid(false).statusCode("TIMESTAMP_EXPIRED")
                    .partnerId(normPartner).keyId(keyId).reason("Invalid numeric timestamp").build();
        }

        if (Math.abs(now.getEpochSecond() - ts) > MAX_TIMESTAMP_DRIFT_SECONDS) {
            return VerificationResult.builder().valid(false).statusCode("TIMESTAMP_EXPIRED")
                    .partnerId(normPartner).keyId(keyId).reason("Timestamp drift exceeded 300s replay tolerance").build();
        }

        // Nonce replay defense inside tolerance window
        String cleanNonce = nonce.trim();
        if (nonceRepo.findByPartnerIdAndNonce(normPartner, cleanNonce).isPresent()) {
            return VerificationResult.builder().valid(false).statusCode("REPLAY_DETECTED")
                    .partnerId(normPartner).keyId(keyId).reason("Nonce already consumed within window").build();
        }

        PartnerWebhookKey key = (keyId != null && !keyId.isBlank())
                ? keyRepo.findByKeyId(keyId.trim()).orElse(null)
                : keyRepo.findFirstByPartnerIdAndStatusOrderByCreatedAtDesc(normPartner, "ACTIVE").orElse(null);

        if (key == null || !key.getPartnerId().equalsIgnoreCase(normPartner)) {
            return VerificationResult.builder().valid(false).statusCode("INVALID_SIGNATURE")
                    .partnerId(normPartner).keyId(keyId).reason("Unknown signing key for partner").build();
        }
        if ("REVOKED".equalsIgnoreCase(key.getStatus())) {
            return VerificationResult.builder().valid(false).statusCode("KEY_REVOKED")
                    .partnerId(normPartner).keyId(key.getKeyId()).keyStatus("REVOKED")
                    .reason("Signing key revoked: " + key.getRevokedReason()).build();
        }
        if ("ROTATING_GRACE".equalsIgnoreCase(key.getStatus())
                && key.getGraceUntil() != null && now.isAfter(key.getGraceUntil())) {
            return VerificationResult.builder().valid(false).statusCode("KEY_REVOKED")
                    .partnerId(normPartner).keyId(key.getKeyId()).keyStatus("EXPIRED_GRACE")
                    .reason("Rotation grace window expired").build();
        }

        String canonical = buildCanonicalPayload(httpMethod, path, ts, cleanNonce, rawBody);
        String expectedSig = signCanonical(key.getSecretHex(), canonical);
        String providedSig = signatureHeader.trim().toLowerCase(Locale.ROOT);
        if (providedSig.startsWith("sha256=")) {
            providedSig = providedSig.substring("sha256=".length());
        }

        boolean matches = MessageDigest.isEqual(
                expectedSig.getBytes(StandardCharsets.UTF_8),
                providedSig.getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            return VerificationResult.builder().valid(false).statusCode("INVALID_SIGNATURE")
                    .partnerId(normPartner).keyId(key.getKeyId()).keyStatus(key.getStatus())
                    .reason("HMAC-SHA256 canonical digest mismatch").build();
        }

        nonceRepo.save(WebhookNonceJournal.builder()
                .partnerId(normPartner)
                .nonce(cleanNonce)
                .expiresAt(now.plusSeconds(MAX_TIMESTAMP_DRIFT_SECONDS))
                .build());

        return VerificationResult.builder()
                .valid(true)
                .statusCode("VERIFIED")
                .partnerId(normPartner)
                .keyId(key.getKeyId())
                .keyStatus(key.getStatus())
                .reason("Signature and nonce verified")
                .build();
    }

    public Map<String, String> signOutboundWebhook(String partnerId, String method, String path, String rawBody, Instant nowOverride) {
        String normPartner = partnerId.trim().toUpperCase(Locale.ROOT);
        PartnerWebhookKey active = keyRepo.findFirstByPartnerIdAndStatusOrderByCreatedAtDesc(normPartner, "ACTIVE")
                .orElseThrow(() -> new IllegalStateException("No ACTIVE signing key for partner " + normPartner));
        Instant now = nowOverride != null ? nowOverride : Instant.now();
        long ts = now.getEpochSecond();
        String nonce = Long.toHexString(RNG.nextLong()) + Long.toHexString(ts);
        String canonical = buildCanonicalPayload(method, path, ts, nonce, rawBody);
        String sig = "sha256=" + signCanonical(active.getSecretHex(), canonical);
        return Map.of(
                "X-Partner-ID", normPartner,
                "X-Key-ID", active.getKeyId(),
                "X-Timestamp", String.valueOf(ts),
                "X-Nonce", nonce,
                "X-Signature", sig
        );
    }

    public static String buildCanonicalPayload(String method, String path, long timestampSec, String nonce, String rawBody) {
        return (method != null ? method.toUpperCase(Locale.ROOT) : "POST")
                + "\n" + (path != null ? path : "/")
                + "\n" + timestampSec
                + "\n" + nonce
                + "\n" + (rawBody != null ? rawBody : "");
    }

    public static String signCanonical(String secretHex, String canonicalPayload) {
        try {
            byte[] keyBytes = HexFormat.of().parseHex(secretHex.trim());
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            byte[] digest = mac.doFinal(canonicalPayload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new RuntimeException("Failed computing HMAC-SHA256", ex);
        }
    }
}
