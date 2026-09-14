package com.manacommunity.api.dto;

import lombok.Data;

@Data
public class SportsAuctionTeamSummary {
    Long   teamId;
    String teamName;
    String colorHex;
    Long   remainingBudget;
    int    playerCount;
}
