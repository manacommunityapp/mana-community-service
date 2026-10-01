package com.manacommunity.api.sports.dto;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

import java.util.Map;

public record SportsPlayerMatchStatsResponse(
    Long playerId,
    String playerName,
    String teamName,
    String sportType,
    Map<String, Object> stats
) {}
