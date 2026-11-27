package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a duplicate notification or idempotency check is triggered.
 * Client message is generic and never echoes idempotency keys or PII.
 */
public class NotificationAlreadyProcessedException extends ManaCommunityException {

    public NotificationAlreadyProcessedException() {
        super("This notification has already been processed.",
                HttpStatus.CONFLICT, "NOTIFICATION_ALREADY_PROCESSED");
    }

    public NotificationAlreadyProcessedException(String idempotencyKey) {
        super("This notification has already been processed.",
                HttpStatus.CONFLICT, "NOTIFICATION_ALREADY_PROCESSED");
    }
}
