package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an external SMS gateway fails.
 * Client message is generic and never leaks gateway credentials, account SIDs, or phone numbers.
 */
public class SmsProviderException extends ManaCommunityException {

    public SmsProviderException(String provider, String detail) {
        super("SMS delivery service temporarily unavailable. Please try again later.",
                HttpStatus.BAD_GATEWAY, "SMS_PROVIDER_ERROR");
    }

    public SmsProviderException(String provider, String detail, Throwable cause) {
        super("SMS delivery service temporarily unavailable. Please try again later.",
                HttpStatus.BAD_GATEWAY, "SMS_PROVIDER_ERROR", cause);
    }
}
