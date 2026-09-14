package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SportsPlayerRankingResponse {
    private Long id;
    private PlayerRef player;
    private SportRef sport;
    private Long communityId;
    private Integer rank;
    private Integer rating;
    private String source;
    private String season;
    private String notes;

    @Data @Builder
    public static class PlayerRef {
        private Long id;
        private String fullName;
        private String email;
        private String flatNo;
        private String avatarUrl;
    }

    @Data @Builder
    public static class SportRef {
        private Long id;
        private String name;
        private String icon;
    }
}
