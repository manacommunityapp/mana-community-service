package com.manacommunity.api.dto.scheduler;

import java.util.Map;

public record SportsPlayerMatchStatsResponse(
    Long playerId,
    String playerName,
    String teamName,
    String sportType,
    Map<String, Object> stats
) {}
