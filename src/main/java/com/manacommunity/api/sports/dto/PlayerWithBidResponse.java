package com.manacommunity.api.sports.dto;

import java.time.LocalDateTime;

public record PlayerWithBidResponse(
    Long    playerId,
    String  playerName,
    String  category,
    String  playerRole,
    Integer age,
    Integer basePrice,
    String  statsJson,
    Long    currentBid,
    Long    nextBid,
    Integer nextIncrement,
    String  currentBidTeamName,
    int     queueOrder,
    String  status,
    Integer innings,
    String  bestBowling,
    String  cricHeroesId,
    String  cricHeroesUrl,
    LocalDateTime verifiedAt
) {}
