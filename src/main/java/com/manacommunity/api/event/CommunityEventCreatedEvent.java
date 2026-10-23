package com.manacommunity.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Published when a community event (sports fixture, social event, etc.) is
 * created or updated. Notifies all members of the community.
 *
 * Usage (in EventService or EventController):
 * <pre>
 *   events.publishEvent(new CommunityEventCreatedEvent(this,
 *       savedEvent.getId(),
 *       savedEvent.getTitle(),
 *       savedEvent.getVenue(),
 *       savedEvent.getStartAt().toString(),
 *       community.getId()));
 * </pre>
 */
@Getter
public class CommunityEventCreatedEvent extends ApplicationEvent {

    private final Long   eventId;
    private final String eventTitle;
    private final String venue;
    private final String startAt;    // ISO-8601 string for display in the notification
    private final Long   communityId;

    public CommunityEventCreatedEvent(Object source,
                                      Long eventId,
                                      String eventTitle,
                                      String venue,
                                      String startAt,
                                      Long communityId) {
        super(source);
        this.eventId     = eventId;
        this.eventTitle  = eventTitle;
        this.venue       = venue;
        this.startAt     = startAt;
        this.communityId = communityId;
    }
}
