package com.manacommunity.api.dto;

public record SportsAuctionStatsResponse(
    long totalPlayers,
    long soldPlayers,
    long queuedPlayers,
    long totalTeams,
    long totalBudget,
    long totalSpent
) {}
