package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

public class SmsTemplateNotFoundException extends ManaCommunityException {
    public SmsTemplateNotFoundException(String templateCode) {
        super("SMS template not found: " + templateCode, HttpStatus.NOT_FOUND, "SMS_TEMPLATE_NOT_FOUND");
    }
}
