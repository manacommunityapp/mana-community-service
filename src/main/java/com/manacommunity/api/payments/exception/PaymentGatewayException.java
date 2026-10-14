package com.manacommunity.api.payments.exception;

import com.manacommunity.api.exception.ManaCommunityException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when the Razorpay API call fails (network error, invalid credentials,
 * gateway timeout, etc.).  The raw Razorpay error is logged but NOT propagated
 * to the caller to avoid leaking internal details.
 */
public class PaymentGatewayException extends ManaCommunityException {

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_GATEWAY_ERROR", cause);
    }

    public PaymentGatewayException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_GATEWAY_ERROR");
    }
}
