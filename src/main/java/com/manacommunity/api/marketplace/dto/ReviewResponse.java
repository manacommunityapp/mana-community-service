package com.manacommunity.api.marketplace.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {

    private Long id;
    private Long listingId;
    private String listingTitle;
    private ReviewerRef reviewer;
    private int rating;
    private String comment;
    private String sellerReply;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class ReviewerRef {
        private Long id;
        private String fullName;
        private boolean verified;
    }
}
