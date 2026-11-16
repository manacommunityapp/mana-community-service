package com.manacommunity.api.payments.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class PaymentGatewayNotAllowedException extends RuntimeException {
    public PaymentGatewayNotAllowedException(String message) {
        super(message);
    }
}
