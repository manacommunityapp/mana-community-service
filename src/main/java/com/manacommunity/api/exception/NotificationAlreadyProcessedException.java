package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

public class NotificationAlreadyProcessedException extends ManaCommunityException {
    public NotificationAlreadyProcessedException(String idempotencyKey) {
        super("Notification already processed: " + idempotencyKey, HttpStatus.CONFLICT, "NOTIFICATION_ALREADY_PROCESSED");
    }
}
