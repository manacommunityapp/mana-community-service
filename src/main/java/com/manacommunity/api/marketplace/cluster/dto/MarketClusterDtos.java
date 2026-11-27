package com.manacommunity.api.marketplace.cluster.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MarketClusterDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateClusterListingRequest {
        private String title;
        private String description;
        private String category;
        private BigDecimal price;
        private String visibilityScope; // INTRA_SOCIETY, CLUSTER_3KM
        private BigDecimal latitude;
        private BigDecimal longitude;
        private int durationDays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankedListingResponse {
        private Long id;
        private Long communityId;
        private String communityName;
        private String title;
        private String description;
        private String category;
        private BigDecimal price;
        private String visibilityScope;
        private BigDecimal distanceMeters;
        private boolean isSameSociety;
        private double rankingScore;
        private double spatialScore;
        private double freshnessScore;
        private double trustScore;
        private String moderationStatus;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HyperlocalFeedQuery {
        private Long userCommunityId;
        private BigDecimal userLatitude;
        private BigDecimal userLongitude;
        private String category;
        private int limit;
    }
}
