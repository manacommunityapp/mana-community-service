package com.manacommunity.api.dto.scheduler;

import jakarta.validation.constraints.NotNull;

public record SportsMatchResultRequest(
    @NotNull Long    matchId,
    Long             winnerTeamId,
    String           scoreTeamA,
    String           scoreTeamB,
    String           matchNotes,
    Integer          runsTeamA,
    Integer          runsTeamB,
    Integer          oversTeamA,
    Integer          oversTeamB
) {}
