package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception thrown when a subsystem or downstream component is degraded,
 * tripped by a circuit breaker, or temporarily unavailable.
 * Ensures no fake/sample fallback data is returned to the user in production.
 */
public class ServiceDegradedException extends ManaCommunityException {

    private final String subsystem;

    public ServiceDegradedException(String message, String subsystem, String errorCode) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, errorCode);
        this.subsystem = subsystem;
    }

    public ServiceDegradedException(String message, String subsystem, String errorCode, Throwable cause) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, errorCode, cause);
        this.subsystem = subsystem;
    }

    public String getSubsystem() {
        return subsystem;
    }
}
