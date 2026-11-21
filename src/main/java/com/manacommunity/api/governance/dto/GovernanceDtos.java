package com.manacommunity.api.governance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

public class GovernanceDtos {

    @Data @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StatsDto {
        private long upcomingMeetings;
        private long activeProposals;
        private long openVotes;
        private long passedResolutions;
        private long actionItemsOpen;
        private long myMeetingsAttended;
        private long myVotesRecorded;
    }

    @Data @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MeetingDto {
        private String id;
        private String type;
        private String title;
        private String description;
        private String date;
        private String time;
        private String location;
        private String meetingLink;
        private String status;
        private int quorumConfirmed;
        private int quorumRequired;
        private double quorumPercentage;
        private boolean isQuorumAchieved;
        private boolean hasUserConfirmed;
        private String attendanceMode;
        private String proxyNominee;
        private String annualReportUrl;
        private String minutesUrl;
        private List<Object> agenda;
        private List<Object> liveMinutes;
    }

    @Data @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProposalDto {
        private String id;
        private String title;
        private String description;
        private String category;
        private String type;
        private String estimatedCost;
        private String submittedBy;
        private String submittedDate;
        private int supportCount;
        private int supportThreshold;
        private boolean hasUserSupported;
        private String status;
        private List<String> pros;
        private List<String> cons;
        private String committeeNotes;
    }

    @Data @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BallotDto {
        private String id;
        private String title;
        private String category;
        private String description;
        private String resolutionMotionText;
        private String startDate;
        private String endDate;
        private int totalEligibleVoters;
        private int totalVotesCast;
        private double turnoutPercentage;
        private int minQuorumPercentage;
        private boolean isQuorumReached;
        private boolean hasUserVoted;
        private String userVotedOptionId;
        private String voteReceiptHash;
        private String status;
        private List<Map<String, Object>> options;
    }

    @Data @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResolutionDto {
        private String id;
        private String resolutionNumber;
        private String title;
        private String meetingReference;
        private String passedDate;
        private String category;
        private String approvedBudget;
        private String status;
        private Map<String, Object> votingSummary;
        private List<String> keyProvisions;
        private String presidingOfficer;
        private String legalBindingStatement;
        private String documentUrl;
    }

    @Data @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VaultDocumentDto {
        private String id;
        private String title;
        private String category;
        private String fileSize;
        private String fileFormat;
        private String lastUpdated;
        private String documentUrl;
        private boolean isConfidential;
        private String resolutionRef;
        private boolean digitalSealVerified;
    }
}
