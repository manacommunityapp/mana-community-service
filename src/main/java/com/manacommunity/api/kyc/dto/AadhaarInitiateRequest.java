package com.manacommunity.api.kyc.dto;

import lombok.Data;

/**
 * Body of {@code POST /api/kyc/aadhaar/initiate}.
 *
 * <p>{@code aadhaarHash} is the lowercase hex SHA-256 the device computes over the
 * normalised Aadhaar number (whitespace and hyphens stripped, then upper-cased). It is
 * forwarded to the Aadhaar authority as sent, and re-protected with the server pepper
 * before being stored — see {@code KycIdNumberProtector}.
 */
@Data
public class AadhaarInitiateRequest {

    /** Lowercase hex SHA-256 of the normalised Aadhaar number, computed client-side. */
    private String aadhaarHash;

    /** Last 1–4 digits of the Aadhaar number. Display only; optional. */
    private String aadhaarLast4;

    private Long communityId;

    /**
     * Retired field. Bound only so an old client sending a raw Aadhaar number gets an
     * explicit 400 rather than having it accepted and hashed server-side.
     */
    private String aadhaarNumber;
}
