package com.manacommunity.api.sports.dto;

public record CricketCareerStatsResponse(
        Long userId,
        String playerName,
        BattingStats batting,
        BowlingStats bowling
) {

    public record BattingStats(
            int matches,
            int innings,
            int notOuts,
            int runs,
            String highScore,
            double average,
            double strikeRate,
            int centuries,
            int fifties,
            int fours,
            int sixes
    ) {}

    public record BowlingStats(
            int matches,
            int innings,
            int wickets,
            int runsConceded,
            String oversBowled,
            Double average,
            double economy,
            Double strikeRate,
            int fiveWickets,
            String bestBowling
    ) {}
}
