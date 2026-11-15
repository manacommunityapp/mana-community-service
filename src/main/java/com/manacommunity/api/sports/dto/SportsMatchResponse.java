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
    boolean isBye,
    String  teamAFlat,
    String  teamBFlat,
    String  placeholderTeamA,
    String  placeholderTeamB
) {
    public SportsMatchResponse(
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
    ) {
        this(matchId, roundName, matchNumber, bracketSlot, teamAId, teamAName, teamBId, teamBName,
             teamAColor, teamBColor, scheduledAt, venueId, venueName, courtId, courtName,
             status, scoreTeamA, scoreTeamB, winnerName, winnerAdvancesToMatchId, isBye, null, null, null, null);
    }

    public SportsMatchResponse(
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
        boolean isBye,
        String  teamAFlat,
        String  teamBFlat
    ) {
        this(matchId, roundName, matchNumber, bracketSlot, teamAId, teamAName, teamBId, teamBName,
             teamAColor, teamBColor, scheduledAt, venueId, venueName, courtId, courtName,
             status, scoreTeamA, scoreTeamB, winnerName, winnerAdvancesToMatchId, isBye, teamAFlat, teamBFlat, null, null);
    }
}
