package com.manacommunity.api.dto;

public record SportsScheduleStatsResponse(
        long totalGames,
        long liveNow,
        long upcoming,
        long completed
) {}
