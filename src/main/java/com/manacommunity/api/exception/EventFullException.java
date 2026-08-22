package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

/** Thrown when the maximum participant limit for an event has been reached. */
public class EventFullException extends ManaCommunityException {

    public EventFullException(String eventName, int maxParticipants) {
        super("'" + eventName + "' has reached its maximum capacity of "
                + maxParticipants + " participants. Registration is closed. Contact admin for manual registration.",
                HttpStatus.CONFLICT, "EVENT_FULL");
    }
}
