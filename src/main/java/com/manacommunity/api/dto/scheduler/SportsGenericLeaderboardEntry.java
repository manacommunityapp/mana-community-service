package com.manacommunity.api.dto.scheduler;

public record SportsGenericLeaderboardEntry(
    Long playerId,
    String playerName,
    Long teamId,
    String teamName,
    String category,
    Integer value,
    Integer matchesPlayed,
    Integer rank
) {}
