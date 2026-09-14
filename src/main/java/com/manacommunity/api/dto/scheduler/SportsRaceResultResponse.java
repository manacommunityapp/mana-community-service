package com.manacommunity.api.dto.scheduler;

import java.util.List;

public record SportsRaceResultResponse(
    Long id,
    Long matchId,
    Long playerId,
    String playerName,
    Long teamId,
    String teamName,
    Integer heatNumber,
    Integer laneNumber,
    Long finishTimeMillis,
    String formattedTime,
    String raceStatus,
    Integer overallRank,
    Integer heatRank,
    Long personalBestMillis,
    Boolean isPersonalBest,
    List<String> splitTimes,
    String notes
) {}
