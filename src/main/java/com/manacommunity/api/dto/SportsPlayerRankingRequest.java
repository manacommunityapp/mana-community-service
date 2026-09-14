package com.manacommunity.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SportsPlayerRankingRequest(
        @NotNull Long userId,
        @NotNull Long sportId,
        @NotNull Long communityId,
        @Min(1) Integer rank,
        @Min(0) Integer rating,
        @Size(max = 20) String season,
        @Size(max = 2000) String notes
) {
    public String resolvedSeason() {
        return (season == null || season.isBlank()) ? "CURRENT" : season;
    }
}
