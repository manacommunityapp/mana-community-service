package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GovernanceStatsResponse {
    private long upcomingMeetings;
    private long activeProposals;
    private long openVotes;
    private long passedResolutions;
    private long actionItemsOpen;
    private long myMeetingsAttended;
    private long myVotesRecorded;
    private double communityQuorumRate;
}
