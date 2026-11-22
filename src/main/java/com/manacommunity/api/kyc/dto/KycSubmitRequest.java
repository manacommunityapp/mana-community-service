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

    private String frontImageBase64;

    private String backImageBase64;

    /**
     * Retired field. Bound only so that an old client sending a raw number gets an
     * explicit 400 instead of having the number silently accepted and hashed here;
     * it is never read for storage. Removing it from the DTO would make such a
     * request succeed with an empty hash on the default Jackson settings.
     */
    private String documentNumber;
}
