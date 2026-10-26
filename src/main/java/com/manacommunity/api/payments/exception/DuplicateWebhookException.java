package com.manacommunity.api.payments.exception;

import com.manacommunity.api.exception.ManaCommunityException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a Razorpay webhook event with the same event ID has already been
 * processed (idempotency guard).  The service catches this and returns HTTP 200
 * so Razorpay does not keep retrying.
 */
public class DuplicateWebhookException extends ManaCommunityException {

    public DuplicateWebhookException(String eventId) {
        super("Webhook event already processed: " + eventId,
              HttpStatus.OK, "DUPLICATE_WEBHOOK_EVENT");
    }
}
