package com.manacommunity.api.address.normalization.model;

/**
 * Resolved canonical database record mapping.
 */
public record CanonicalAddressResolution(
        Long communityId,
        String communityName,
        Long towerId,
        String towerName,
        Long flatId,
        String flatNumber
) {}
