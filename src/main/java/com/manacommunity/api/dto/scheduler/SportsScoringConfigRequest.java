package com.manacommunity.api.dto.scheduler;

public record SportsScoringConfigRequest(
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
