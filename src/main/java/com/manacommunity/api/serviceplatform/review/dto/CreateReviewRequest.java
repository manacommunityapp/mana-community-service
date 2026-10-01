package com.manacommunity.api.serviceplatform.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequest {
    @NotNull
    private Long workOrderId;
    @NotNull
    private Long reviewerId;
    @Min(1)
    @Max(5)
    private int rating;
    private Integer qualityRating;
    private Integer punctualityRating;
    private Integer behaviorRating;
    private String comment;
}
