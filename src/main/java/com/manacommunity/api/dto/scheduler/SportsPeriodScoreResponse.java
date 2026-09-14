package com.manacommunity.api.dto.scheduler;

public record SportsPeriodScoreResponse(
    Integer periodNumber,
    String periodLabel,
    Integer scoreTeamA,
    Integer scoreTeamB
) {}
