package com.manacommunity.api.dto.scheduler;

public record SportsGenericScoreResponse(
    Long id,
    Long matchId,
    Long teamId,
    String teamName,
    Long playerId,
    String playerName,
    String eventType,
    Integer periodNumber,
    Integer matchMinute,
    Integer pointsAwarded,
    String description,
    Long secondaryPlayerId,
    String secondaryPlayerName,
    String timestamp
) {}
