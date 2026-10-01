package com.manacommunity.api.serviceplatform.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderMatchScoreDto {
    private Long providerId;
    private String providerName;
    private double overallScore;
    private double ratingScore;
    private double completedJobsScore;
    private double distanceScore;
    private String reason;
}
