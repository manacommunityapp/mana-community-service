package com.manacommunity.api.sports.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SportsMatchDeltaEvent(
    Long matchId,
    Long sequence,
    String eventType,
    String sportType,
    String matchStatus,
    Integer currentInnings,
    ScoreSummary score,
    LastBallSummary lastBall,
    StrikerSummary striker,
    NonStrikerSummary nonStriker,
    BowlerSummary bowler,
    String description,
    Instant timestamp
) {
    public record ScoreSummary(
        String teamAName,
        String teamBName,
        String scoreTeamA,
        String scoreTeamB,
        Integer runs,
        Integer wickets,
        String overs,
        String runRate,
        Integer target,
        Integer leadOrTrail
    ) {}

    public record LastBallSummary(
        Integer ballNumber,
        Integer overNumber,
        Integer deliveryNumber,
        Integer runsScored,
        Integer extrasRuns,
        String extrasType,
        Boolean isBoundary,
        Boolean isSix,
        Boolean isWicket,
        String dismissalType,
        String commentary
    ) {}

    public record StrikerSummary(
        Long playerId,
        String playerName,
        Integer runs,
        Integer balls,
        Integer fours,
        Integer sixes,
        String strikeRate
    ) {}

    public record NonStrikerSummary(
        Long playerId,
        String playerName,
        Integer runs,
        Integer balls
    ) {}

    public record BowlerSummary(
        Long playerId,
        String playerName,
        String overs,
        Integer maidens,
        Integer runs,
        Integer wickets,
        String economy
    ) {}
}
