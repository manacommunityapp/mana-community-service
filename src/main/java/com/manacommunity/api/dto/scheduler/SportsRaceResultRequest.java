package com.manacommunity.api.dto.scheduler;

import java.util.List;

public record SportsRaceResultRequest(
    Long matchId,
    Long playerId,
    Long teamId,
    Integer heatNumber,
    Integer laneNumber,
    Long finishTimeMillis,
    String formattedTime,
    String raceStatus,
    List<String> splitTimes,
    String notes
) {}
