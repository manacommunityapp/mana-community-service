package com.manacommunity.api.sports.dto;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

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
