package com.manacommunity.api.registration.external.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable token payload signed cryptographically for external public registration.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SignedRegistrationToken(
        String tokenId,
        Long communityId,
        RegistrationScope scope,
        Long referenceId,
        String scopeTitle,
        int maxUses,
        Instant expiresAt,
        Map<String, String> metadata
) {
    @JsonIgnore
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
