package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public record SportsAuctionTeamRequest(
    @NotNull Long configId,
    @NotBlank String teamName,
    @NotBlank String ownerName,
    Long ownerUserId,
    String colorHex,
    @NotNull @Min(1000) Long totalBudget
) {}
