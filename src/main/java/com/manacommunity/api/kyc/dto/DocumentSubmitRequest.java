package com.manacommunity.api.kyc.dto;

import lombok.Data;

/**
 * Body of {@code POST /api/kyc/document} (manual document review).
 *
 * <p>{@code idNumberHash} is the lowercase hex SHA-256 the device computes over the
 * normalised ID number (whitespace and hyphens stripped, then upper-cased). It is
 * re-protected with the server pepper before storage — see {@code KycIdNumberProtector}.
 */
@Data
public class DocumentSubmitRequest {

    private Long communityId;

    private String idType;

    /** Lowercase hex SHA-256 of the normalised ID number, computed client-side. */
    private String idNumberHash;

    /** Last 1–4 characters of the ID number. Display only. */
    private String idNumberLast4;

    private String documentUrl;

    /**
     * Retired field. Bound only so an old client sending a raw number gets an explicit
     * 400 rather than having it accepted and hashed server-side.
     */
    private String idNumber;
}
