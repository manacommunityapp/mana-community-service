package com.manacommunity.api.kyc.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ServiceDegradedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.regex.Pattern;

/**
 * Validates the government-ID digest the client sends, and converts it into the value
 * this service is allowed to persist.
 *
 * <h3>Why the client digest is not stored as-is</h3>
 * The mobile client sends {@code SHA-256(normalised ID number)} so that the raw number
 * never crosses the wire. That protects the <em>transport</em>, but it is not a
 * confidentiality control for the <em>database</em>: Aadhaar is 12 digits, so the whole
 * input space is 10^12 candidates and a plain SHA-256 of every one of them can be
 * enumerated offline in milliseconds. PAN is not materially better. Anyone who reads a
 * dump of the stored digests therefore recovers every ID number.
 *
 * <p>So the digest is re-protected server-side before it is written:
 * {@code HMAC-SHA256(key = server pepper, message = client digest)}. Without the pepper
 * the offline enumeration above has no target, and the pepper lives only in the
 * environment ({@code MANA_KYC_ID_PEPPER}), never in the database or this repository.
 *
 * <p>HMAC rather than Argon2/scrypt because KYC lookups must stay fast and
 * deterministic, and the input is already a fixed-length, high-entropy-per-byte digest
 * — a memory-hard KDF would buy little here and cost every request. A per-row salt is
 * deliberately <em>not</em> used: it would make two submissions of the same ID number
 * hash differently and break the equality lookups the KYC flow depends on. A
 * server-wide pepper is the shape that keeps lookups working while still defeating
 * offline enumeration.
 *
 * <h3>Fail closed</h3>
 * If no pepper is configured this class refuses to produce a storable value and the
 * request fails with 503. It never falls back to storing the bare client digest —
 * that fallback would silently reintroduce the enumerable-dump problem above, and it
 * would also poison the column with values that cannot be matched against peppered
 * ones later.
 */
@Slf4j
@Service
public class KycIdNumberProtector {

    /** Configuration key holding the server-wide pepper (env {@code MANA_KYC_ID_PEPPER}). */
    public static final String PEPPER_PROPERTY = "mana.kyc.id-pepper";

    /** The client sends a lowercase hex SHA-256 digest and nothing else. */
    private static final Pattern CLIENT_DIGEST_PATTERN = Pattern.compile("^[0-9a-f]{64}$");

    private static final int LAST4_MAX_LENGTH = 4;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private final String pepper;

    public KycIdNumberProtector(@Value("${" + PEPPER_PROPERTY + ":}") String configuredPepper) {
        this.pepper = configuredPepper == null ? "" : configuredPepper.trim();
        if (this.pepper.isEmpty()) {
            log.error("*** '{}' (env MANA_KYC_ID_PEPPER) is not set. KYC ID submission will be "
                            + "refused with HTTP 503 until it is. The bare client-side SHA-256 is NOT "
                            + "stored as a fallback: a 12-digit Aadhaar digest is trivially enumerable "
                            + "offline, so storing it unpeppered would be equivalent to storing the "
                            + "number itself. ***",
                    PEPPER_PROPERTY);
        } else {
            log.info("KYC ID digests will be re-protected with HMAC-SHA256 using the pepper from {}",
                    PEPPER_PROPERTY);
        }
    }

    /** True when a pepper is configured and ID digests can be stored. */
    public boolean isConfigured() {
        return !pepper.isEmpty();
    }

    /**
     * Refuses a request that still carries a raw, unhashed ID number.
     *
     * <p>Hashing it here instead would mean the raw number had already been transmitted,
     * logged by every proxy on the way in and held in this JVM's heap — exactly what the
     * client change removed. The client must hash before sending, so an old client gets a
     * 400 that names the fields it should be sending.
     *
     * @param rawValue       the legacy raw-number field as bound from the request body
     * @param legacyField    its JSON name, for the error message
     * @param expectedFields the JSON names the client should send instead
     */
    public void rejectRawIdNumber(String rawValue, String legacyField, String expectedFields) {
        if (rawValue != null && !rawValue.isBlank()) {
            throw new InvalidInputException(
                    "'" + legacyField + "' is no longer accepted: raw government ID numbers must not be "
                            + "transmitted. Send " + expectedFields + " instead, where the hash is the "
                            + "lowercase hex SHA-256 of the ID number with whitespace and hyphens removed "
                            + "and the remainder upper-cased. Please update the app.");
        }
    }

    /**
     * Validates the shape of a client-supplied digest and returns it unchanged.
     * Use this when the digest is handed to an external verification API rather than stored.
     */
    public String requireClientDigest(String clientDigest, String fieldName) {
        if (clientDigest == null || clientDigest.isBlank()) {
            throw new InvalidInputException("'" + fieldName + "' is required (lowercase hex SHA-256 "
                    + "of the normalised government ID number).");
        }
        String trimmed = clientDigest.trim();
        if (!CLIENT_DIGEST_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidInputException("'" + fieldName + "' must be a 64-character lowercase "
                    + "hex SHA-256 digest (pattern ^[0-9a-f]{64}$).");
        }
        return trimmed;
    }

    /** Validates the display-only last-4 fragment: 1–4 characters, no surrounding blanks. */
    public String requireLast4(String last4, String fieldName) {
        if (last4 == null || last4.isBlank()) {
            throw new InvalidInputException("'" + fieldName + "' is required (the last 1–4 characters "
                    + "of the government ID number, for masked display).");
        }
        String trimmed = last4.trim();
        if (trimmed.length() > LAST4_MAX_LENGTH) {
            throw new InvalidInputException(
                    "'" + fieldName + "' must be 1–4 characters; got " + trimmed.length() + ".");
        }
        return trimmed;
    }

    /**
     * Validates the client digest and returns the value to persist:
     * {@code HMAC-SHA256(key = server pepper, message = client digest)}, lowercase hex.
     *
     * @throws InvalidInputException    (400) when the digest is missing or malformed
     * @throws ServiceDegradedException (503) when no server pepper is configured
     */
    public String protectForStorage(String clientDigest, String fieldName) {
        String validated = requireClientDigest(clientDigest, fieldName);

        if (!isConfigured()) {
            log.error("Refusing to store a KYC ID digest: '{}' (env MANA_KYC_ID_PEPPER) is not "
                    + "configured. Storing the bare client digest is not an option — see "
                    + "KycIdNumberProtector for why.", PEPPER_PROPERTY);
            throw new ServiceDegradedException(
                    "KYC ID storage is unavailable: the server-side ID pepper is not configured. "
                            + "No identity data has been stored. Contact support.",
                    "KYC", "KYC_ID_PEPPER_NOT_CONFIGURED");
        }

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return toHex(mac.doFinal(validated.getBytes(StandardCharsets.US_ASCII)));
        } catch (GeneralSecurityException e) {
            // HmacSHA256 is mandatory in every JRE, so this is a broken runtime, not bad input.
            log.error("HMAC-SHA256 unavailable — cannot protect KYC ID digest", e);
            throw new ServiceDegradedException(
                    "KYC ID storage is unavailable. No identity data has been stored.",
                    "KYC", "KYC_ID_HMAC_UNAVAILABLE", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            out[i * 2] = HEX_DIGITS[b >>> 4];
            out[i * 2 + 1] = HEX_DIGITS[b & 0x0F];
        }
        return new String(out);
    }
}
