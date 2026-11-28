package com.manacommunity.api.registration.external.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.manacommunity.api.registration.external.model.SignedRegistrationToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * Cryptographic signing and validation engine for public external registration links.
 * Produces URL-safe HMAC-SHA256 signed tokens ensuring zero link tampering.
 */
@Service
@Slf4j
public class ExternalRegistrationLinkCryptoService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final byte[] secretKeyBytes;
    private final ObjectMapper objectMapper;

    public ExternalRegistrationLinkCryptoService(
            @Value("${mana.registration.link.secret:mana-community-default-secure-hmac-sha256-key-32bytes-min}") String secret
    ) {
        this.secretKeyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Signs a registration token and formats it as Base64Url(payload) + "." + Base64Url(hmac).
     */
    public String signToken(SignedRegistrationToken token) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(token);
            String payloadBase64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(jsonPayload.getBytes(StandardCharsets.UTF_8));

            byte[] hmac = computeHmac(payloadBase64.getBytes(StandardCharsets.UTF_8));
            String signatureBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(hmac);

            return payloadBase64 + "." + signatureBase64;
        } catch (Exception e) {
            log.error("[CryptoService] Failed to sign registration token: {}", e.getMessage(), e);
            throw new IllegalStateException("Could not generate cryptographic link signature", e);
        }
    }

    /**
     * Validates signature and verifies token is not expired.
     */
    public SignedRegistrationToken verifyAndParseToken(String signedToken) {
        if (signedToken == null || !signedToken.contains(".")) {
            throw new IllegalArgumentException("Invalid signed token format: missing signature delimiter");
        }

        String[] parts = signedToken.split("\\.");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid signed token format: expected payload.signature");
        }

        String payloadBase64 = parts[0];
        String signatureBase64 = parts[1];

        // 1. Verify HMAC Signature in constant time to prevent timing attacks
        byte[] expectedHmac = computeHmac(payloadBase64.getBytes(StandardCharsets.UTF_8));
        byte[] receivedHmac;
        try {
            receivedHmac = Base64.getUrlDecoder().decode(signatureBase64);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed token signature encoding", e);
        }

        if (!MessageDigest.isEqual(expectedHmac, receivedHmac)) {
            log.warn("[CryptoService] Signature mismatch detected! Link tampering attempt.");
            throw new SecurityException("Cryptographic signature validation failed. Link has been tampered with.");
        }

        // 2. Deserialize JSON payload
        SignedRegistrationToken token;
        try {
            byte[] jsonBytes = Base64.getUrlDecoder().decode(payloadBase64);
            token = objectMapper.readValue(jsonBytes, SignedRegistrationToken.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed token JSON payload", e);
        }

        // 3. Check Expiry
        if (token.isExpired()) {
            throw new IllegalStateException("Registration link expired at " + token.expiresAt() + ". Please request a fresh link.");
        }

        return token;
    }

    private byte[] computeHmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretKeyBytes, HMAC_ALGORITHM));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC SHA-256 calculation error", e);
        }
    }
}
