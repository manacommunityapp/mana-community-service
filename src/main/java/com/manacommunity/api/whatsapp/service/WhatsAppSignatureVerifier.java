package com.manacommunity.api.whatsapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Validates Meta WhatsApp Cloud API Webhook Signatures (HMAC-SHA256)
 * and verification challenge tokens.
 */
@Component
@Slf4j
public class WhatsAppSignatureVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final String appSecret;
    private final String verifyToken;

    public WhatsAppSignatureVerifier(
            @Value("${whatsapp.cloud.app-secret:default-secret-test-key-32bytes}") String appSecret,
            @Value("${whatsapp.cloud.verify-token:mana-mygate-webhook-token}") String verifyToken) {
        this.appSecret = appSecret;
        this.verifyToken = verifyToken;
    }

    /**
     * Verifies the initial GET challenge request from Meta.
     */
    public boolean verifyChallenge(String mode, String token) {
        return "subscribe".equals(mode) && verifyToken.equals(token);
    }

    /**
     * Verifies the POST payload signature against the X-Hub-Signature-256 header.
     * Uses constant-time comparison to prevent timing side-channel attacks.
     */
    public boolean verifySignature(String rawPayload, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            log.warn("Missing or invalid X-Hub-Signature-256 header: {}", signatureHeader);
            return false;
        }

        String providedSignature = signatureHeader.substring(SIGNATURE_PREFIX.length());

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hmacBytes) {
                hex.append(String.format("%02x", b));
            }
            String calculatedSignature = hex.toString();

            // Constant-time comparison
            return MessageDigest.isEqual(
                    calculatedSignature.getBytes(StandardCharsets.UTF_8),
                    providedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception ex) {
            log.error("Error computing HMAC-SHA256 for WhatsApp webhook: {}", ex.getMessage());
            return false;
        }
    }
}
