package com.manacommunity.api.exception;

/**
 * Thrown when an emergency alert or SOS dispatch cannot be confirmed via primary or escalation channels.
 * Triggers high-priority security audit and escalation procedures.
 */
public class EmergencyDispatchFailedException extends ServiceDegradedException {

    public EmergencyDispatchFailedException(String message) {
        super(message, "EMERGENCY", "EMERGENCY_DISPATCH_FAILED");
    }

    public EmergencyDispatchFailedException(String message, Throwable cause) {
        super(message, "EMERGENCY", "EMERGENCY_DISPATCH_FAILED", cause);
    }
}
