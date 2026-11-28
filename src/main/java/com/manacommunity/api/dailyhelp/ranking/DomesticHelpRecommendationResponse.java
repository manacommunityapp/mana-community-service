package com.manacommunity.api.dailyhelp.ranking;

import lombok.Builder;

import java.util.List;

@Builder
public record DomesticHelpRecommendationResponse(
        Long communityId,
        String category,
        String residentTower,
        List<RankedWorkerDto> recommendedWorkers,
        int totalCandidatesEvaluated,
        int coldStartExplorersCount,
        long executionTimeMs
) {}
