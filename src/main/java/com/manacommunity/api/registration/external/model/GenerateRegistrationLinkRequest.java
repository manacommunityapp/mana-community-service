package com.manacommunity.api.registration.external.model;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Builder
public record GenerateRegistrationLinkRequest(
        @NotNull Long communityId,
        @NotNull RegistrationScope scope,
        Long referenceId,
        String scopeTitle,
        int maxUses,
        @NotNull @Future Instant expiresAt,
        Map<String, String> metadata
) {}
