package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

public class InvalidPhoneNumberException extends ManaCommunityException {
    public InvalidPhoneNumberException(String phone) {
        super("Invalid phone number: " + phone, HttpStatus.BAD_REQUEST, "INVALID_PHONE_NUMBER");
    }
}
