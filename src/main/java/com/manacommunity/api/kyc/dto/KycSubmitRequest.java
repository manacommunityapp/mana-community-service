package com.manacommunity.api.kyc.dto;

import lombok.Data;

/**
 * Body of {@code POST /api/kyc/submit} and {@code PUT /api/kyc/documents/{id}}.
 *
 * <p>The raw {@code documentNumber} was removed when the mobile client stopped
 * transmitting government ID numbers. The client now sends a digest plus a display
 * fragment. Client-side normalisation, which the server must mirror exactly or no
 * lookup will ever match:
 * <pre>
 *   strip all whitespace and hyphens  (/[\s-]/g)
 *   upper-case                        (PAN is alphanumeric and the field auto-capitalises)
 *   SHA-256
 *   lowercase hex
 * </pre>
 */
@Data
public class KycSubmitRequest {

    /** AADHAAR | PAN | PASSPORT | VOTER_ID | DRIVING_LICENSE. */
    private String documentType;

    /**
     * Lowercase hex SHA-256 of the normalised ID number, computed on the device.
     * Validated against {@code ^[0-9a-f]{64}$}. Re-protected with the server pepper
     * before it is persisted — see {@code KycIdNumberProtector}.
     */
    private String idNumberHash;

    /** Last 1–4 characters of the ID number. Display only: the UI renders {@code •••• •••• {last4}}. */
    private String idNumberLast4;

    /**
     * Base64 of the document front, with or without a {@code data:image/...;base64,} prefix.
     * Required on {@code POST /api/kyc/submit}, optional on a resubmission (the image already
     * on file is kept when it is omitted).
     *
     * <p>Decoded, sniffed with Tika and stored by {@code KycDocumentImageStore}, which fills
     * {@code KycDocument.frontImageUrl}. Max 5 MB decoded; JPEG / PNG / WebP / HEIC / HEIF
     * only. Anything else is a 400 and no KYC row is written. These fields used to be bound
     * and read by nothing, so the images were silently discarded.
     */
    private String frontImageBase64;

    /** Base64 of the document back. Optional — PAN and passport have nothing on the reverse. */
    private String backImageBase64;

    /**
     * Retired field. Bound only so that an old client sending a raw number gets an
     * explicit 400 instead of having the number silently accepted and hashed here;
     * it is never read for storage. Removing it from the DTO would make such a
     * request succeed with an empty hash on the default Jackson settings.
     */
    private String documentNumber;
}
