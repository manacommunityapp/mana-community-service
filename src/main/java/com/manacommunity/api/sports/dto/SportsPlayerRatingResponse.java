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
    private List<String> badges;
    private int suggestedBasePrice;
    private Map<String, Double> breakdown;
}
