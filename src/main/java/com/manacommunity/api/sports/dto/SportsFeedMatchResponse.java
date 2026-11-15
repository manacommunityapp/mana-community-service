package com.manacommunity.api.sports.dto;

/**
 * A match shown in the community feed "Live Now" strip. Contains only what the
 * strip needs; full detail is available through the match live view.
 */
public record SportsFeedMatchResponse(
    Long    matchId,
    Long    configId,
    String  sportName,
    String  tournamentName,
    String  eventName,
    String  roundName,
    String  teamAName,
    String  teamBName,
    String  teamAFlat,
    String  teamBFlat,
    String  scoreTeamA,
    String  scoreTeamB,
    String  status,          // LIVE | COMPLETED
    String  winnerName,
    String  scheduledAt,
    String  completedAt,
    String  venueName
) {}
