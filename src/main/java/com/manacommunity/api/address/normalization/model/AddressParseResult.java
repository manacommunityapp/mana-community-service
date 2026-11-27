package com.manacommunity.api.address.normalization.model;

/**
 * Structured tokens extracted from raw unstructured address text.
 */
public record AddressParseResult(
        String flatNumber,
        String towerOrBlock,
        String societyQuery,
        String locality,
        String city,
        String pincode,
        String normalizedString
) {}
