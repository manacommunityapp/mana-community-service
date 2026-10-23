package com.manacommunity.api.sports.dto;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.dto.SportsTournamentGroupResponse;
import com.manacommunity.api.sports.dto.RoundResponse;


import java.util.List;

public record SportsTournamentScheduleResponse(
    Long                       configId,
    String                     tournamentName,
    String                     tournamentType,
    int                        totalTeams,
    int                        totalMatches,
    int                        totalRounds,
    java.time.LocalDate        startDate,
    java.time.LocalDate        endDate,
    List<SportsTournamentGroupResponse>        groups,
    List<RoundResponse>        rounds,
    List<SportsMatchResponse>        allMatches
) {}
