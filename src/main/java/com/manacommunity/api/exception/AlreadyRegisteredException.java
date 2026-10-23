package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a user is already registered for an event. */
public class AlreadyRegisteredException extends ManaCommunityException {

    public AlreadyRegisteredException(String eventName) {
        super("You are already registered for the event: '" + eventName + "'.",
                HttpStatus.CONFLICT, "ALREADY_REGISTERED");
    }

    /** Use when a fully-formed user-facing message is needed (e.g. cross-seva duplicate check). */
    public AlreadyRegisteredException(String eventName, String reason) {
        super(reason, HttpStatus.CONFLICT, "ALREADY_REGISTERED");
    }
}
