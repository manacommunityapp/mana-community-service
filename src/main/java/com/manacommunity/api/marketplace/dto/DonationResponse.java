package com.manacommunity.api.marketplace.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DonationResponse {

    private Long id;
    private String title;
    private String description;
    private String category;
    private String condition;
    private String status;
    private String imageUrl;
    private DonorRef donor;
    private Long communityId;
    private String claimedByName;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class DonorRef {
        private Long id;
        private String fullName;
    }
}
