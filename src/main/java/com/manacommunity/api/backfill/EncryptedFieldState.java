package com.manacommunity.api.backfill;

/**
 * Classification of a single raw column value read straight out of Postgres
 * (i.e. <em>before</em> {@code EncryptedStringConverter} has touched it).
 *
 * @see EncryptedFieldInspector for the detection rule and why it is shaped this way
 */
public enum EncryptedFieldState {

    /** Column is NULL — nothing to encrypt, nothing to decide. */
    NOT_SET,

    /** Confidently legacy plaintext: must be re-written so the converter encrypts it. */
    PLAINTEXT,

    /** Confidently AES-256-GCM ciphertext under the current key: leave alone. */
    ENCRYPTED,

    /**
     * Structurally indistinguishable from ciphertext (valid Base64, long enough to hold
     * IV + GCM tag) but the GCM tag does not verify under the current key. Could be
     * plaintext that happens to be valid Base64, or real ciphertext written under a
     * different/rotated key. The job refuses to guess and skips the whole row.
     */
    AMBIGUOUS
}
