package com.manacommunity.api.sports.dto;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

public record SportsPeriodScoreResponse(
    Integer periodNumber,
    String periodLabel,
    Integer scoreTeamA,
    Integer scoreTeamB
) {}
