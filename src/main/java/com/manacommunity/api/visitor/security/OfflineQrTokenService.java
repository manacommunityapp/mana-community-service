package com.manacommunity.api.visitor.security;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Ed25519 asymmetric signer & verifier for offline guard-tablet QR validation.
 * Private key never leaves the backend; guard tablets cache the X.509 Ed25519
 * public key at shift login to validate visitor passes in &lt;1ms even without internet.
 */
@Slf4j
@Service
public class OfflineQrTokenService {

    private static final String TOKEN_PREFIX = "v1";
    private final KeyPair keyPair;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public OfflineQrTokenService() {
        this(generateEd25519KeyPair(), Clock.systemUTC());
    }

    public OfflineQrTokenService(KeyPair keyPair, Clock clock) {
        this.keyPair = keyPair;
        this.clock = clock;
    }

    private static KeyPair generateEd25519KeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Ed25519 algorithm unavailable on this JVM", ex);
        }
    }

    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    /**
     * Generates a compact signed token:
     * {@code v1.<base64url(jti|communityId|passCode|flat|expEpochSec)>.<base64url_ed25519_sig>}
     */
    public String signToken(Long communityId, String passCode, String flatNumber, long ttlSeconds) {
        String jti = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        long exp = Instant.now(clock).plusSeconds(Math.max(60L, ttlSeconds)).getEpochSecond();
        String safeFlat = flatNumber != null ? flatNumber.replace("|", "_") : "NA";
        String safeCode = passCode != null ? passCode.replace("|", "_") : "NA";
        long safeComm = communityId != null ? communityId : 0L;

        String rawPayload = jti + "|" + safeComm + "|" + safeCode + "|" + safeFlat + "|" + exp;
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(rawPayload.getBytes(StandardCharsets.UTF_8));

        try {
            Signature sig = Signature.getInstance("Ed25519");
            sig.initSign(keyPair.getPrivate());
            sig.update(encodedPayload.getBytes(StandardCharsets.UTF_8));
            String encodedSig = Base64.getUrlEncoder().withoutPadding().encodeToString(sig.sign());
            return TOKEN_PREFIX + "." + encodedPayload + "." + encodedSig;
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to sign Ed25519 offline QR token", ex);
        }
    }

    /**
     * Verifies signature, tenant community match, and expiry using only the public key.
     */
    public VerifiedQrToken verifyOfflineToken(String token, long expectedCommunityId) {
        return verifyWithPublicKey(token, keyPair.getPublic(), expectedCommunityId, clock);
    }

    public static VerifiedQrToken verifyWithPublicKeyBase64(
            String token,
            String publicKeyBase64,
            long expectedCommunityId,
            Clock clock) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            PublicKey pub = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(keyBytes));
            return verifyWithPublicKey(token, pub, expectedCommunityId, clock);
        } catch (Exception ex) {
            return VerifiedQrToken.builder().valid(false).reason("Invalid public key format").build();
        }
    }

    private static VerifiedQrToken verifyWithPublicKey(
            String token,
            PublicKey publicKey,
            long expectedCommunityId,
            Clock clock) {
        if (token == null || token.isBlank()) {
            return VerifiedQrToken.builder().valid(false).reason("Empty QR token").build();
        }
        String[] parts = token.trim().split("\\.");
        if (parts.length != 3 || !TOKEN_PREFIX.equals(parts[0])) {
            return VerifiedQrToken.builder().valid(false).reason("Unsupported QR token format").build();
        }

        try {
            Signature sig = Signature.getInstance("Ed25519");
            sig.initVerify(publicKey);
            sig.update(parts[1].getBytes(StandardCharsets.UTF_8));
            byte[] providedSig = Base64.getUrlDecoder().decode(parts[2]);

            if (!sig.verify(providedSig)) {
                return VerifiedQrToken.builder().valid(false).reason("Invalid cryptographic signature").build();
            }

            String decoded = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            String[] fields = decoded.split("\\|", -1);
            if (fields.length < 5) {
                return VerifiedQrToken.builder().valid(false).reason("Malformed token claims").build();
            }

            String jti = fields[0];
            long commId = Long.parseLong(fields[1]);
            String passCode = fields[2];
            String flat = fields[3];
            long exp = Long.parseLong(fields[4]);

            if (commId != expectedCommunityId) {
                return VerifiedQrToken.builder().valid(false).reason("Cross-community QR rejected").build();
            }
            if (Instant.now(clock).getEpochSecond() > exp) {
                return VerifiedQrToken.builder().valid(false).jti(jti).passCode(passCode).reason("QR token expired").build();
            }

            return VerifiedQrToken.builder()
                    .valid(true)
                    .jti(jti)
                    .communityId(commId)
                    .passCode(passCode)
                    .flatNumber(flat)
                    .expiresAtEpochSec(exp)
                    .build();
        } catch (Exception ex) {
            return VerifiedQrToken.builder().valid(false).reason("Token verification failed").build();
        }
    }

    @Data
    @Builder
    public static class VerifiedQrToken {
        private boolean valid;
        private String jti;
        private Long communityId;
        private String passCode;
        private String flatNumber;
        private long expiresAtEpochSec;
        private String reason;
    }
}
