package com.manacommunity.api.backfill;

import com.manacommunity.api.security.EncryptedStringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * Decides whether a raw column value is already AES-256-GCM ciphertext.
 *
 * <h3>Why this is not trivial</h3>
 * {@code FieldEncryptionService.decrypt(String)} is deliberately tolerant: when the input
 * is not Base64, is too short, or fails GCM tag verification, it returns the input
 * <em>unchanged</em> so that legacy plaintext rows keep reading correctly. That tolerance
 * means "call decrypt and see if it worked" is not, on its own, a test for ciphertext —
 * a successful no-op and a successful decryption look the same from the outside unless we
 * compare the result against the input.
 *
 * <h3>The detection rule</h3>
 * A value is treated as {@link EncryptedFieldState#ENCRYPTED} only when ALL THREE hold:
 * <ol>
 *   <li>it decodes as strict Base64 (the exact encoding {@code encrypt()} emits), AND</li>
 *   <li>the decoded payload is at least {@value #MIN_CIPHERTEXT_BYTES} bytes — 12-byte GCM
 *       IV + 16-byte GCM tag, the smallest possible output of {@code encrypt("")}, AND</li>
 *   <li>passing it through the converter returns something <em>different</em> from the input.
 *       Because the converter returns its input verbatim on any decryption failure, a
 *       different result can only mean the GCM tag verified under the current key.</li>
 * </ol>
 *
 * Failing (1) or (2) proves the value cannot be ciphertext this service produced, so it is
 * {@link EncryptedFieldState#PLAINTEXT} and safe to encrypt.
 *
 * Passing (1) and (2) but failing (3) is {@link EncryptedFieldState#AMBIGUOUS}: it is either
 * plaintext that happens to look like Base64 (e.g. a long alphanumeric medical id) or real
 * ciphertext written under a key we no longer hold. Encrypting the second case would
 * double-wrap data that can still be recovered with the old key and make it permanently
 * unreadable, so an ambiguous value is never written — the whole row is skipped and
 * reported for a human to look at. This is why classification runs on the RAW column value
 * rather than on the entity attribute: once JPA has loaded the entity, the converter has
 * already collapsed "legacy plaintext" and "decrypted ciphertext" into the same string and
 * the distinction is gone.
 *
 * <p>False-positive risk of rule (3): a plaintext value whose bytes happen to authenticate
 * as GCM ciphertext under our key is a 2^-128 event, and such a value would be skipped
 * (left as-is), never corrupted.
 *
 * <p>Note this class never touches {@code Cipher} or {@code FieldEncryptionService.encrypt}
 * directly; the only crypto it performs is a read through the JPA converter.
 */
@Component
@RequiredArgsConstructor
public class EncryptedFieldInspector {

    /** 12-byte GCM IV + 16-byte GCM tag; see FieldEncryptionService. */
    static final int MIN_CIPHERTEXT_BYTES = 12 + 16;

    private final EncryptedStringConverter converter;

    public EncryptedFieldState classify(String rawColumnValue) {
        if (rawColumnValue == null) {
            return EncryptedFieldState.NOT_SET;
        }

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(rawColumnValue);
        } catch (IllegalArgumentException notBase64) {
            return EncryptedFieldState.PLAINTEXT;   // e.g. "Peanuts, shellfish"
        }
        if (decoded.length < MIN_CIPHERTEXT_BYTES) {
            return EncryptedFieldState.PLAINTEXT;   // too short to be IV + tag
        }

        // The converter returns its input unchanged when the GCM tag does not verify,
        // so "came back different" is the tag-verification signal.
        String roundTripped = converter.convertToEntityAttribute(rawColumnValue);
        if (roundTripped != null && !roundTripped.equals(rawColumnValue)) {
            return EncryptedFieldState.ENCRYPTED;
        }
        return EncryptedFieldState.AMBIGUOUS;
    }
}
