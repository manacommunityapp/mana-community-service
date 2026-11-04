package com.manacommunity.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Thrown when handover verification attempts exceed maximum allowed attempts (e.g. 5 failed OTPs).
 * Locks out further automated verification to mitigate brute-force guessing.
 */
@Getter
public class HandoverLockoutException extends ManaCommunityException {

    private final String groupReference;
    private final int failedAttempts;
    private final int maxAttempts;

    public HandoverLockoutException(String groupReference, int failedAttempts, int maxAttempts) {
        super(String.format("Handover verification locked out for group %s after %d failed attempts (max: %d). Supervisor override required.",
                groupReference, failedAttempts, maxAttempts), HttpStatus.TOO_MANY_REQUESTS, "HANDOVER_LOCKED_OUT");
        this.groupReference = groupReference;
        this.failedAttempts = failedAttempts;
        this.maxAttempts = maxAttempts;
    }
}
