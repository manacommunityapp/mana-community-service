package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when SMS rate limit is exceeded.
 * Client message is generic and never echoes phone number or rate limiting keys.
 */
public class SmsRateLimitException extends ManaCommunityException {

    public SmsRateLimitException() {
        super("SMS rate limit exceeded. Please wait a few minutes before trying again.",
                HttpStatus.TOO_MANY_REQUESTS, "SMS_RATE_LIMIT_EXCEEDED");
    }

    public SmsRateLimitException(String key) {
        super("SMS rate limit exceeded. Please wait a few minutes before trying again.",
                HttpStatus.TOO_MANY_REQUESTS, "SMS_RATE_LIMIT_EXCEEDED");
    }
}
