package com.manacommunity.api.sports.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SportsTeamCompositionResponse {
    private Long teamId;
    private int totalPlayers;
    private int batters;
    private int bowlers;
    private int allRounders;
    private int wicketKeepers;
    private int leftHandBats;
    private int rightHandBats;
    private int pacers;
    private int spinners;
    private long totalRuns;
    private int totalWickets;
    private double avgBattingAverage;
    private double avgBowlingEconomy;
    private double avgStrikeRate;
    private long budgetSpent;
    private long budgetRemaining;
}
