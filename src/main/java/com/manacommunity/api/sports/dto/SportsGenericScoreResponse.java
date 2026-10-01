package com.manacommunity.api.sports.dto;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

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
