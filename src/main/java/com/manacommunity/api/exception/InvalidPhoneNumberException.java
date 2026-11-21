package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a phone number validation or normalization fails.
 * Client message is generic and never echoes the raw phone number to protect personal information.
 */
public class InvalidPhoneNumberException extends ManaCommunityException {

    public InvalidPhoneNumberException() {
        super("Invalid phone number format. Please provide a valid 10-digit mobile number.",
                HttpStatus.BAD_REQUEST, "INVALID_PHONE_NUMBER");
    }

    public InvalidPhoneNumberException(String phone) {
        super("Invalid phone number format. Please provide a valid 10-digit mobile number.",
                HttpStatus.BAD_REQUEST, "INVALID_PHONE_NUMBER");
    }
}
