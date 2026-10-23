package com.manacommunity.api.sports.dto;

public record SportsScheduleStatsResponse(
        long totalGames,
        long liveNow,
        long upcoming,
        long completed
) {}
