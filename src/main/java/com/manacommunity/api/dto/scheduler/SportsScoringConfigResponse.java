package com.manacommunity.api.dto.scheduler;

public record SportsScoringConfigResponse(
    Long id,
    Long configId,
    String sportType,
    Integer periodsCount,
    Integer pointsToWinPeriod,
    Boolean mustWinByTwo,
    Integer periodsToWin,
    Integer periodDurationMinutes,
    Boolean hasOvertime,
    Boolean hasPenaltyShootout,
    Integer tiebreakPointsToWin,
    String scoringRulesJson
) {}
