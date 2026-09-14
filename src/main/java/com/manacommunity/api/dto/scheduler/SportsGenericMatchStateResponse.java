package com.manacommunity.api.dto.scheduler;

import java.util.List;

public record SportsGenericMatchStateResponse(
    Long matchId,
    String status,
    String sportType,
    Long teamAId,
    String teamAName,
    String teamAColor,
    Long teamBId,
    String teamBName,
    String teamBColor,
    Integer currentPeriod,
    Integer scoreTeamA,
    Integer scoreTeamB,
    Integer periodsWonA,
    Integer periodsWonB,
    List<SportsPeriodScoreResponse> periods,
    List<SportsGenericScoreResponse> recentEvents,
    SportsScoringConfigResponse scoringConfig
) {}
