package com.manacommunity.api.dailyhelp.ranking;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record DomesticHelpRankingRequest(
        @NotNull Long communityId,
        Long residentFlatId,
        String residentTower,
        String residentWing,
        @NotNull String category,
        String requestedTimeSlot,
        Integer limit
) {
    public int resolvedLimit() {
        return (limit != null && limit > 0 && limit <= 50) ? limit : 20;
    }
}
