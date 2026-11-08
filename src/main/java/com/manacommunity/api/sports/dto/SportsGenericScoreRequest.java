package com.manacommunity.api.sports.dto;

import java.util.UUID;

public record SportsGenericScoreRequest(
    Long matchId,
    Long teamId,
    Long playerId,
    String eventType,
    Integer periodNumber,
    Integer matchMinute,
    Integer pointsAwarded,
    String description,
    Long secondaryPlayerId,
    UUID commandId
) {
    public SportsGenericScoreRequest {
        if (pointsAwarded == null) pointsAwarded = 1;
    }

    public SportsGenericScoreRequest(
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
        this(matchId, teamId, playerId, eventType, periodNumber, matchMinute, pointsAwarded, description, secondaryPlayerId, null);
    }
}
