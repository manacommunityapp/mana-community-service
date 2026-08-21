package com.manacommunity.api.notification.enums;

/** Lifecycle states of an SmsMessage record. */
public enum SmsStatus {
    CREATED,    // record inserted, not yet queued
    QUEUED,     // submitted to async processor
    SENDING,    // provider call in progress
    SENT,       // provider accepted (but delivery not yet confirmed)
    DELIVERED,  // provider delivery receipt received
    FAILED,     // send attempt failed; eligible for retry
    RETRYING,   // scheduled for the next retry attempt
    DLQ,        // exceeded max retries; moved to dead-letter state
    CANCELLED;  // manually cancelled before sending

    public boolean isTerminal() {
        return this == DELIVERED || this == DLQ || this == CANCELLED;
    }

    public boolean isRetryable() {
        return this == FAILED || this == RETRYING;
    }
}
