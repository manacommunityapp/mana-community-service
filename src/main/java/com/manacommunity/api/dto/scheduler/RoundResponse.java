package com.manacommunity.api.dto.scheduler;

import java.util.List;

public record RoundResponse(
    String              roundName,
    int                 roundNumber,
    List<SportsMatchResponse> matches
) {}
