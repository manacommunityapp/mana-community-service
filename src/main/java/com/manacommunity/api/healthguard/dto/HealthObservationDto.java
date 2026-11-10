package com.manacommunity.api.healthguard.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
public record HealthObservationDto(
        Long id,
        String provider,
        String sourceRecordId,
        String sourceApp,
        String type,
        BigDecimal numericValue,
        String unit,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        OffsetDateTime lastReceivedAt
) {}
