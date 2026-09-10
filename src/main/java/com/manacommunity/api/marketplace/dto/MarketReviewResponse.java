package com.manacommunity.api.marketplace.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketReviewResponse {

    private Long id;
    private Long listingId;
    private String reviewerName;
    private Long reviewerId;
    private int rating;
    private String comment;
    private String sellerReply;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
