package com.manacommunity.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * Published by MatchNotificationScheduler for matches starting in ~30 minutes.
 * Notifies all team members of both teams so they can be ready.
 *
 * The scheduler sets notificationSentAt on the match entity after publishing
 * so the same match is never re-notified.
 */
@Getter
public class MatchStartingSoonEvent extends ApplicationEvent {

    private final Long         matchId;
    private final String       matchTitle;       // "Team A vs Team B"
    private final String       sport;
    private final String       venue;
    private final String       scheduledAt;      // ISO-8601 for display
    private final List<Long>   allPlayerIds;     // both teams combined
    private final Long         communityId;

    public MatchStartingSoonEvent(Object source,
                                  Long matchId,
                                  String matchTitle,
                                  String sport,
                                  String venue,
                                  String scheduledAt,
                                  List<Long> allPlayerIds,
                                  Long communityId) {
        super(source);
        this.matchId      = matchId;
        this.matchTitle   = matchTitle;
        this.sport        = sport;
        this.venue        = venue;
        this.scheduledAt  = scheduledAt;
        this.allPlayerIds = allPlayerIds != null ? allPlayerIds : List.of();
        this.communityId  = communityId;
    }
}
