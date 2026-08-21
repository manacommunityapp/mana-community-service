package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

public class SmsProviderException extends ManaCommunityException {
    public SmsProviderException(String provider, String detail) {
        super("SMS provider error [" + provider + "]: " + detail, HttpStatus.BAD_GATEWAY, "SMS_PROVIDER_ERROR");
    }
    public SmsProviderException(String provider, String detail, Throwable cause) {
        super("SMS provider error [" + provider + "]: " + detail, HttpStatus.BAD_GATEWAY, "SMS_PROVIDER_ERROR", cause);
    }
}
