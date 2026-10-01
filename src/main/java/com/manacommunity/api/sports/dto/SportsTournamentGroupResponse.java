package com.manacommunity.api.sports.dto;
import com.manacommunity.api.sports.dto.SportsMatchResponse;


import java.util.List;

public record SportsTournamentGroupResponse(
    Long                       groupId,
    String                     groupName,
    List<StandingResponse>     standings,
    List<SportsMatchResponse>        matches
) {}
