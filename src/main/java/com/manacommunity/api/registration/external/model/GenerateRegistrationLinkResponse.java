package com.manacommunity.api.registration.external.model;

import lombok.Builder;

import java.time.Instant;

@Builder
public record GenerateRegistrationLinkResponse(
        String tokenId,
        String signedToken,
        String registrationUrl,
        RegistrationScope scope,
        Long referenceId,
        String scopeTitle,
        int maxUses,
        Instant expiresAt,
        String status
) {}
