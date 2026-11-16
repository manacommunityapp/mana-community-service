package com.manacommunity.api.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.exception.ManaCommunityException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

/**
 * Service to support client-side asymmetric password encryption using RSA-OAEP SHA-256.
 * Generates an RSA key pair, distributes the public key in PEM format, and decrypts
 * payloads received from browser Web Crypto API.
 */
@Service
public class RsaKeyService {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyService.class);
    private static final String ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;
    private static final String CIPHER_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final long MAX_AGE_MS = 5 * 60 * 1000L; // 5 minutes validity

    private KeyPair keyPair;
    private String publicKeyPem;
    private String keyId;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
            generator.initialize(KEY_SIZE, new SecureRandom());
            this.keyPair = generator.generateKeyPair();
            this.keyId = "key-" + System.currentTimeMillis();

            byte[] pubBytes = keyPair.getPublic().getEncoded();
            String base64 = Base64.getEncoder().encodeToString(pubBytes);
            StringBuilder pem = new StringBuilder();
            pem.append("-----BEGIN PUBLIC KEY-----\n");
            int i = 0;
            while (i < base64.length()) {
                int end = Math.min(i + 64, base64.length());
                pem.append(base64, i, end).append("\n");
                i = end;
            }
            pem.append("-----END PUBLIC KEY-----");
            this.publicKeyPem = pem.toString();

            log.info("RsaKeyService initialized RSA-{} key pair with keyId={}", KEY_SIZE, keyId);
        } catch (Exception e) {
            log.error("Failed to initialize RSA key pair for auth encryption", e);
            throw new IllegalStateException("RSA key generation failed", e);
        }
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public String getKeyId() {
        return keyId;
    }

    /**
     * Decrypts an RSA-OAEP SHA-256 encrypted base64 payload.
     * Unpacks JSON format: {"p": "<password>", "t": <timestamp>}
     * Validates timestamp freshness to protect against replay attacks.
     */
    public String decryptPassword(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isBlank()) {
            throw new ManaCommunityException("Password payload is empty", HttpStatus.BAD_REQUEST, "INVALID_CREDENTIALS");
        }

        try {
            byte[] cipherBytes = Base64.getDecoder().decode(encryptedBase64.trim());
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    PSource.PSpecified.DEFAULT
            );
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate(), oaepParams);
            byte[] decrypted = cipher.doFinal(cipherBytes);
            String decryptedStr = new String(decrypted, StandardCharsets.UTF_8);

            // Check if structured JSON with timestamp
            if (decryptedStr.startsWith("{") && decryptedStr.endsWith("}")) {
                try {
                    JsonNode node = objectMapper.readTree(decryptedStr);
                    if (node.has("p")) {
                        if (node.has("t")) {
                            long ts = node.get("t").asLong();
                            long now = System.currentTimeMillis();
                            if (Math.abs(now - ts) > MAX_AGE_MS) {
                                log.warn("Rejected expired login payload timestamp={} now={}", ts, now);
                                throw new ManaCommunityException(
                                        "Authentication request has expired. Please try again.",
                                        HttpStatus.BAD_REQUEST,
                                        "REQUEST_EXPIRED"
                                );
                            }
                        }
                        return node.get("p").asText();
                    }
                } catch (ManaCommunityException mce) {
                    throw mce;
                } catch (Exception ex) {
                    log.debug("Decrypted payload is not JSON with p attribute, returning raw string");
                }
            }

            return decryptedStr;
        } catch (ManaCommunityException mce) {
            throw mce;
        } catch (Exception e) {
            log.error("RSA decryption failed for auth password: {}", e.getMessage());
            throw new ManaCommunityException("Invalid or expired encryption token", HttpStatus.BAD_REQUEST, "DECRYPTION_FAILED");
        }
    }

    // Public helper method for testing encryption with the generated public key
    public byte[] encryptForTesting(byte[] plainBytes) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT
        );
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic(), oaepParams);
        return cipher.doFinal(plainBytes);
    }
}

