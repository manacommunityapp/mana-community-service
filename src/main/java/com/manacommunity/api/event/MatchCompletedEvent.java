package com.manacommunity.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * Published when a match status is set to COMPLETED.
 * Triggers two notifications:
 *  1. All participants → rate your players (peer rating prompt)
 *  2. Community spectators subscribed to this match → "Match ended: X won"
 *
 * Usage (in MatchService, after saving status = COMPLETED):
 * <pre>
 *   events.publishEvent(new MatchCompletedEvent(this,
 *       match.getId(), matchTitle, result,
 *       homeTeamName, awayTeamName,
 *       allParticipantIds, communityId));
 * </pre>
 */
@Getter
public class MatchCompletedEvent extends ApplicationEvent {

    private final Long       matchId;
    private final String     matchTitle;
    private final String     result;          // "Team A won by 23 runs"
    private final String     sport;
    private final List<Long> participantIds;  // players in the match
    private final Long       communityId;

    public MatchCompletedEvent(Object source,
                               Long matchId,
                               String matchTitle,
                               String result,
                               String sport,
                               List<Long> participantIds,
                               Long communityId) {
        super(source);
        this.matchId        = matchId;
        this.matchTitle     = matchTitle;
        this.result         = result;
        this.sport          = sport;
        this.participantIds = participantIds != null ? participantIds : List.of();
        this.communityId    = communityId;
    }
}
