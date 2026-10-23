package com.manacommunity.api.serviceplatform.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long workOrderId;
    private Long providerId;
    private String providerName;
    private Long reviewerId;
    private String reviewerName;
    private int rating;
    private Integer qualityRating;
    private Integer punctualityRating;
    private Integer behaviorRating;
    private String comment;
    private String providerResponse;
    private LocalDateTime responseAt;
    private boolean verifiedResident;
    private LocalDateTime createdAt;
}
