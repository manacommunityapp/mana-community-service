package com.manacommunity.api.notification.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EventReminderEvent {
    private final Long userId;
    private final String phoneNumber;
    private final String eventName;
    private final String eventTime;  // pre-formatted for display
    private final String venueOrLink;
    /** "24h" or "1h" */
    private final String reminderType;
}
