package com.manacommunity.api.sports.dto;

import java.util.List;

public record SportsBatchRescheduleResponse(
    int updatedCount,
    String message,
    List<SportsMatchResponse> updatedMatches
) {}
