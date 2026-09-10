package com.manacommunity.api.dto;

import lombok.Data;

/**
 * BUG FIX: SportsAuctionController referenced SportsAuctionTeamSummary but it never existed.
 * Placeholder DTO until proper team summary logic is built.
 */
@Data
public class SportsAuctionTeamSummary{
        Long teamId;
        String teamName;
        String color;
        Integer remainingBudget;
        int playerCount;
}
