package com.manacommunity.api.serviceplatform.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedServiceDto {
    private Long categoryId;
    private String categoryName;
    private String reason;
    private double confidenceScore;
}
