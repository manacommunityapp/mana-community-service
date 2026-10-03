package com.manacommunity.api.sports.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class SportsPlayerRatingResponse {
    private double overall;
    private String tier;
    /** Star rating (1–5) derived from tier: LEGEND=5, ICON=4, PLATINUM=3, GOLD=2, SILVER/BRONZE=1 */
    private int stars;
    private List<String> badges;
    private int suggestedBasePrice;
    private Map<String, Double> breakdown;
}
