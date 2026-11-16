package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BallotResponse {
    private String id;
    private String title;
    private String category;
    private String description;
    private String resolutionMotionText;
    private String startDate;
    private String endDate;
    private int totalEligibleVoters;
    private long totalVotesCast;
    private double turnoutPercentage;
    private int minQuorumPercentage;
    private boolean isQuorumReached;
    private boolean hasUserVoted;
    private String userVotedOptionId;
    private String voteReceiptHash;
    private String status;
    private List<BallotOptionResponse> options;

    @Data
    @Builder
    public static class BallotOptionResponse {
        private String id;
        private String text;
        private String description;
        private long votesCount;
        private double percentage;
    }
}
