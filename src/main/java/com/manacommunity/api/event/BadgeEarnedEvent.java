package com.manacommunity.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Published by the badge-awarding logic (in MatchService or a dedicated
 * BadgeService) when a player earns a new achievement badge.
 *
 * Sends a personal push notification to the player congratulating them.
 *
 * Usage (after badge is persisted):
 * <pre>
 *   events.publishEvent(new BadgeEarnedEvent(this,
 *       userId, "Century Club", "💯",
 *       "You scored 100+ runs in a tournament match!", "legendary"));
 * </pre>
 */
@Getter
public class BadgeEarnedEvent extends ApplicationEvent {

    private final Long   userId;
    private final String badgeName;
    private final String badgeEmoji;
    private final String description;
    private final String rarity;     // common | rare | epic | legendary

    public BadgeEarnedEvent(Object source,
                            Long userId,
                            String badgeName,
                            String badgeEmoji,
                            String description,
                            String rarity) {
        super(source);
        this.userId      = userId;
        this.badgeName   = badgeName;
        this.badgeEmoji  = badgeEmoji;
        this.description = description;
        this.rarity      = rarity;
    }
}
