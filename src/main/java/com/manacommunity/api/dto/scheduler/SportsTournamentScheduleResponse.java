package com.manacommunity.api.dto.scheduler;

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
    List<GroupResponse>        groups,
    List<RoundResponse>        rounds,
    List<SportsMatchResponse>        allMatches
) {}
