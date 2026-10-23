package com.manacommunity.api.sports.dto;

import jakarta.validation.constraints.NotNull;

public record SoldPlayerRequest(
    @NotNull Long playerId,
    @NotNull Long teamId
) {}
