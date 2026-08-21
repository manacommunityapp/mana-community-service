package com.manacommunity.api.notification.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class EventCancelledEvent {
    private final Long eventId;
    private final String eventName;
    private final LocalDateTime scheduledAt;
    /** User ids of registered attendees — dispatcher resolves phones */
    private final List<Long> affectedUserIds;
}
