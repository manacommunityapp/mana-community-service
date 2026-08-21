package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

public class SmsOtpVerificationException extends ManaCommunityException {
    public SmsOtpVerificationException(String reason) {
        super("OTP verification failed: " + reason, HttpStatus.UNPROCESSABLE_ENTITY, "OTP_VERIFICATION_FAILED");
    }
}
