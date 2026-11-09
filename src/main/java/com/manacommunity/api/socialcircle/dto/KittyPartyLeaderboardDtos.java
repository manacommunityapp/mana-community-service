package com.manacommunity.api.socialcircle.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

public class KittyPartyLeaderboardDtos {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderboardEntry {
        private Long userId;
        private String userName;
        private String flatDetails;
        private String avatarUrl;
        private int rank;
        private int totalGamesPlayed;
        private int totalWins;
        private int totalPoints;
        private Map<String, Integer> winBreakdown;
        private List<String> badges;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CircleLeaderboardResponse {
        private Long circleId;
        private String circleName;
        private int totalGamesInCircle;
        private List<LeaderboardEntry> entries;
    }
}
