package com.manacommunity.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Published when an emergency SOS alert is triggered.
 * Triggers a MAX importance push notification with vibration to all community members.
 */
@Getter
public class EmergencyBroadcastEvent extends ApplicationEvent {

    private final Long communityId;
    private final String title;
    private final String description;
    private final String location;

    public EmergencyBroadcastEvent(Object source,
                                   Long communityId,
                                   String title,
                                   String description,
                                   String location) {
        super(source);
        this.communityId = communityId;
        this.title = title;
        this.description = description;
        this.location = location;
    }
}