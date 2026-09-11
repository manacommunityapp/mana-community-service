package com.manacommunity.api.dto.scheduler;

import java.util.List;

public record GroupResponse(
    Long                       groupId,
    String                     groupName,
    List<StandingResponse>     standings,
    List<SportsMatchResponse>        matches
) {}
