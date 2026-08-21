package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

public class SmsOtpExpiredException extends ManaCommunityException {
    public SmsOtpExpiredException() {
        super("OTP has expired", HttpStatus.GONE, "OTP_EXPIRED");
    }
}
