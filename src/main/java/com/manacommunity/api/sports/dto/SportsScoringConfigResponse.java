package com.manacommunity.api.sports.dto;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

public record SportsScoringConfigResponse(
    Long id,
    Long configId,
    String sportType,
    Integer periodsCount,
    Integer pointsToWinPeriod,
    Boolean mustWinByTwo,
    Integer periodsToWin,
    Integer periodDurationMinutes,
    Boolean hasOvertime,
    Boolean hasPenaltyShootout,
    Integer tiebreakPointsToWin,
    String scoringRulesJson
) {}
