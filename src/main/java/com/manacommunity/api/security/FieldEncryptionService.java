package com.manacommunity.api.security;

import com.manacommunity.api.exception.EncryptionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;

/**
 * AES-256-GCM field-level encryption for sensitive (health / emergency-contact) columns.
 *
 * <h3>Key management</h3>
 * The key is read from {@code mana.encryption.field-key} (env var
 * {@code MANA_ENCRYPTION_FIELD_KEY}). It must be a Base64-encoded 16/24/32-byte key,
 * or a raw 16/24/32-character secret.
 * <p>
 * If the property is missing, the service only falls back to a built-in development key
 * when the active Spring profiles are development profiles ({@code dev}, {@code test},
 * {@code local}) or no profile is active at all. Under any other profile — notably
 * {@code prod} — the application refuses to start, because encrypting production medical
 * data under a key committed to this repository is equivalent to not encrypting it.
 */
@Slf4j
@Component
public class FieldEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private static final int TAG_LENGTH_BYTE = TAG_LENGTH_BIT / 8;

    /** Smallest possible valid ciphertext payload: IV + GCM tag (empty plaintext). */
    private static final int MIN_CIPHERTEXT_BYTES = IV_LENGTH_BYTE + TAG_LENGTH_BYTE;

    private static final String KEY_PROPERTY = "mana.encryption.field-key";

    /** Profiles under which the built-in development key may be used. */
    private static final Set<String> DEV_PROFILES = Set.of("dev", "test", "local", "default");

    /** Costly to construct and thread-safe once seeded — hoisted out of {@link #encrypt}. */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String DEV_KEY_MATERIAL = "ManaDevKey123456ManaDevKey123456";

    private final SecretKey secretKey;

    public FieldEncryptionService(@Value("${" + KEY_PROPERTY + ":}") String configuredKey,
                                 Environment environment) {
        if (configuredKey != null && !configuredKey.isBlank()) {
            this.secretKey = new SecretKeySpec(toKeyBytes(configuredKey.trim()), "AES");
            log.info("Field-level encryption initialised from {}", KEY_PROPERTY);
            return;
        }

        String[] active = environment != null ? environment.getActiveProfiles() : new String[0];
        boolean devOnly = Arrays.stream(active)
                .allMatch(p -> DEV_PROFILES.contains(p.toLowerCase()));

        if (!devOnly) {
            throw new IllegalStateException(
                    "Required property '" + KEY_PROPERTY + "' (env MANA_ENCRYPTION_FIELD_KEY) is not set. "
                            + "Active profiles " + Arrays.toString(active) + " are not development profiles, "
                            + "and the built-in development key must never be used outside development. "
                            + "Supply a Base64-encoded 32-byte AES key.");
        }

        log.warn("*** {} is not set — falling back to the BUILT-IN DEVELOPMENT encryption key. "
                        + "Active profiles: {}. This key is committed to source control and provides NO "
                        + "confidentiality. Never run with this configuration outside development. ***",
                KEY_PROPERTY, Arrays.toString(active));
        this.secretKey = new SecretKeySpec(DEV_KEY_MATERIAL.getBytes(StandardCharsets.UTF_8), "AES");
    }

    /**
     * Accepts either a Base64-encoded key or a raw 16/24/32-character secret, and fails
     * fast on anything that is not a legal AES key length.
     */
    private static byte[] toKeyBytes(String configured) {
        try {
            byte[] decoded = Base64.getDecoder().decode(configured);
            if (isValidAesKeyLength(decoded.length)) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Not Base64 — fall through to the raw-bytes interpretation below.
        }
        byte[] raw = configured.getBytes(StandardCharsets.UTF_8);
        if (isValidAesKeyLength(raw.length)) {
            return raw;
        }
        throw new IllegalStateException("Property '" + KEY_PROPERTY + "' must decode to a 16, 24 or 32 byte "
                + "AES key (Base64-encoded, or supplied as a raw 16/24/32 character secret).");
    }

    private static boolean isValidAesKeyLength(int length) {
        return length == 16 || length == 24 || length == 32;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            // Encryption is NEVER tolerant: a failure here must not degrade into
            // writing plaintext into a column that is documented as encrypted.
            throw new EncryptionException("Encryption failed", e);
        }
    }

    /**
     * Decrypts a stored value, passing through values that are not ciphertext.
     *
     * <p>DELIBERATE: rows written before field-level encryption was introduced (V499 only
     * added column comments, it never back-filled) still hold plaintext. Throwing here would
     * 500 every read of a pre-existing {@code health_profile} / {@code emergency_contact} row,
     * and those rows can never be re-encrypted because a write requires a successful read
     * first. Returning the input unchanged lets legacy rows be read, and they become encrypted
     * on their next write. This is not a swallowed error.
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null) return null;

        byte[] combined;
        try {
            combined = Base64.getDecoder().decode(ciphertext);
        } catch (IllegalArgumentException notBase64) {
            return ciphertext;          // legacy plaintext, e.g. "Peanuts, shellfish"
        }
        if (combined.length < MIN_CIPHERTEXT_BYTES) {
            return ciphertext;          // too short to be IV + GCM tag
        }

        try {
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTE);
            byte[] encrypted = Arrays.copyOfRange(combined, IV_LENGTH_BYTE, combined.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Tag verification failed: the value is legacy plaintext that happens to be
            // valid Base64 (or was written under a different key). Never log the value.
            log.debug("Field value did not authenticate as ciphertext; treating it as legacy plaintext ({})",
                    e.getClass().getSimpleName());
            return ciphertext;
        }
    }
}
