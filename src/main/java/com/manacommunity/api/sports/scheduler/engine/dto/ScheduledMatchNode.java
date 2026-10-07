package com.manacommunity.api.sports.scheduler.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledMatchNode {
    private String matchId;
    private String matchName;
    private int roundIndex;
    private String roundName;
    private int matchNumberInRound;
    private ParticipantNode participantA;
    private ParticipantNode participantB;
    private Long courtId;
    private String courtName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String nextMatchId;
    private boolean isByeMatch;
    private String winnerAdvancesTo;
}
