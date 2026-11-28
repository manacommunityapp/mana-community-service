package com.manacommunity.api.dailyhelp.ranking;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record RankedWorkerDto(
        Long staffProfileId,
        Long affiliationId,
        String staffUuid,
        String displayName,
        String maskedPhone,
        String category,
        BigDecimal aggregateRating,
        Integer ratingSampleSize,
        List<String> assignedTowers,
        int activeContractsCount,
        double proximityScore,
        double attendanceReliabilityScore,
        double neighborEndorsementScore,
        double coldStartBanditSample,
        double finalScore,
        boolean isColdStartExplorer,
        String recommendationBadge,
        String rationale
) {}
