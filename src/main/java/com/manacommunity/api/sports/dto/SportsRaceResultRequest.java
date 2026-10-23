package com.manacommunity.api.sports.dto;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

import java.util.List;

public record SportsRaceResultRequest(
    Long matchId,
    Long playerId,
    Long teamId,
    Integer heatNumber,
    Integer laneNumber,
    Long finishTimeMillis,
    String formattedTime,
    String raceStatus,
    List<String> splitTimes,
    String notes
) {}
