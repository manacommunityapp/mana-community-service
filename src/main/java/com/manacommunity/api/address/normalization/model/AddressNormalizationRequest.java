package com.manacommunity.api.address.normalization.model;

/**
 * Inbound address normalization request payload.
 */
public record AddressNormalizationRequest(
        String rawAddress,
        String cityHint,
        String pincodeHint
) {
    public AddressNormalizationRequest {
        if (rawAddress == null || rawAddress.isBlank()) {
            throw new IllegalArgumentException("rawAddress must not be null or blank");
        }
    }
}
