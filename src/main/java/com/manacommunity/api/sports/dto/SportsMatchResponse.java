package com.manacommunity.api.sports.dto;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

public record SportsMatchResponse(
    Long    matchId,
    String  roundName,
    int     matchNumber,
    int     bracketSlot,
    Long    teamAId,
    String  teamAName,
    Long    teamBId,
    String  teamBName,
    String  teamAColor,
    String  teamBColor,
    String  scheduledAt,
    Long    venueId,
    String  venueName,
    Long    courtId,
    String  courtName,
    String  status,
    String  scoreTeamA,
    String  scoreTeamB,
    String  winnerName,
    Long    winnerAdvancesToMatchId,
    boolean isBye
) {}
