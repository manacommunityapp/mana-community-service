package com.manacommunity.api.sports.dto;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

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
