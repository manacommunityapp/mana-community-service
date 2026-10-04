package com.manacommunity.api.groupbuying.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityAiQueryResponse {
    private String query;
    private List<GroupDealResponse> matchedDeals;
    private List<DemandResponse> matchedDemands;
    private String suggestedAction; // JOIN_DEAL, UPVOTE_DEMAND, CREATE_DEMAND
    private BigDecimal suggestedPrice;
    private BigDecimal estimatedCommunitySavings;
    private Double confidenceScore;
    private String explanation;
}
