package com.manacommunity.api.healthguard.dto;

import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record HealthDataConsentDto(
        String provider,
        String scope,
        String consentVersion,
        OffsetDateTime grantedAt,
        OffsetDateTime revokedAt
) {}
