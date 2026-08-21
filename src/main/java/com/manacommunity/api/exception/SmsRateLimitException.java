package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

public class SmsRateLimitException extends ManaCommunityException {
    public SmsRateLimitException(String key) {
        super("SMS rate limit exceeded for: " + key, HttpStatus.TOO_MANY_REQUESTS, "SMS_RATE_LIMIT_EXCEEDED");
    }
}
