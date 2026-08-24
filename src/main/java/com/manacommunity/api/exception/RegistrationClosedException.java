package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a user tries to register for an event that is not open for registration. */
public class RegistrationClosedException extends ManaCommunityException {

    public RegistrationClosedException(String eventName, String currentStatus) {
        super("Registration for '" + eventName + "' is currently " + currentStatus
                + ". Registrations are only accepted when the event is REGISTRATION_OPEN.",
                HttpStatus.BAD_REQUEST, "REGISTRATION_CLOSED");
    }

    /** Used for booking-window violations where a fully custom message is needed. */
    public RegistrationClosedException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "REGISTRATION_CLOSED");
    }
}
