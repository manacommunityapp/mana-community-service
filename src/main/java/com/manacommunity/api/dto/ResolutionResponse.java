package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ResolutionResponse {
    private String id;
    private String resolutionNumber;
    private String title;
    private String meetingReference;
    private String passedDate;
    private String category;
    private String approvedBudget;
    private String status;
    private VotingSummary votingSummary;
    private List<String> keyProvisions;
    private String presidingOfficer;
    private String legalBindingStatement;
    private String documentUrl;

    @Data
    @Builder
    public static class VotingSummary {
        private int forVotes;
        private int againstVotes;
        private int abstained;
        private double totalPercentageFor;
    }
}
