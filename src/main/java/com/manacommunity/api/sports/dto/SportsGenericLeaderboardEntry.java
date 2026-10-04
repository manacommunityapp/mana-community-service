package com.manacommunity.api.sports.dto;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

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
