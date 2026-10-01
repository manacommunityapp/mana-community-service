package com.manacommunity.api.sports.dto;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

import java.util.List;

public record SportsGenericMatchStateResponse(
    Long matchId,
    String status,
    String sportType,
    Long teamAId,
    String teamAName,
    String teamAColor,
    Long teamBId,
    String teamBName,
    String teamBColor,
    Integer currentPeriod,
    Integer scoreTeamA,
    Integer scoreTeamB,
    Integer periodsWonA,
    Integer periodsWonB,
    List<SportsPeriodScoreResponse> periods,
    List<SportsGenericScoreResponse> recentEvents,
    SportsScoringConfigResponse scoringConfig
) {}
