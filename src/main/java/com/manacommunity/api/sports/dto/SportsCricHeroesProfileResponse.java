package com.manacommunity.api.sports.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class SportsCricHeroesProfileResponse {
    private String cricheroesId;
    private String shareUrl;
    private String resolvedUrl;
    private LocalDateTime verifiedAt;
    private String formatScope;

    private Bio bio;
    private Batting batting;
    private Bowling bowling;
    private Fielding fielding;
    private List<RecentInning> recentForm;

    private Integer mvpPoints;
    private Double rating;

    @Data
    @Builder
    public static class Bio {
        private String fullName;
        private String avatarUrl;
        private String battingStyle;
        private String bowlingStyle;
        private String primaryRole;
    }

    @Data
    @Builder
    public static class Batting {
        private int matches;
        private int innings;
        private int runs;
        private String highestScore;
        private double average;
        private double strikeRate;
        private int fifties;
        private int hundreds;
        private int fours;
        private int sixes;
        private Double boundaryPercentage;
        private Integer dotBallsFaced;
    }

    @Data
    @Builder
    public static class Bowling {
        private int matches;
        private int innings;
        private double overs;
        private int wickets;
        private double economy;
        private double average;
        private double strikeRate;
        private String bestFigures;
        private Double dotBallPercentage;
        private int maidens;
        private int threeWickets;
        private int fiveWickets;
    }

    @Data
    @Builder
    public static class Fielding {
        private int catches;
        private int stumpings;
        private int runOuts;
    }

    @Data
    @Builder
    public static class RecentInning {
        private String matchDate;
        private Integer runs;
        private Integer balls;
        private Integer wickets;
        private Integer runsConceded;
        private Double overs;
        private String opponent;
        private String format;
    }
}
