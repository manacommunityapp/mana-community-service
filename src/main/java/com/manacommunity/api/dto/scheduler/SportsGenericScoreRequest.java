package com.manacommunity.api.dto.scheduler;

public record SportsGenericScoreRequest(
    Long matchId,
    Long teamId,
    Long playerId,
    String eventType,
    Integer periodNumber,
    Integer matchMinute,
    Integer pointsAwarded,
    String description,
    Long secondaryPlayerId
) {
    public SportsGenericScoreRequest {
        if (pointsAwarded == null) pointsAwarded = 1;
    }
}
