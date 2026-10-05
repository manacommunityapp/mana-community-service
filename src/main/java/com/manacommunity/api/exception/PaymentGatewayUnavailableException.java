package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an external payment gateway (e.g. Razorpay, Stripe, UPI) is unreachable or fails.
 * Guarantees that payment failures are never masked by mock success states.
 */
public class PaymentGatewayUnavailableException extends ServiceDegradedException {

    public PaymentGatewayUnavailableException(String message) {
        super(message, "PAYMENT_GATEWAY", "PAYMENT_GATEWAY_UNAVAILABLE");
    }

    public PaymentGatewayUnavailableException(String message, Throwable cause) {
        super(message, "PAYMENT_GATEWAY", "PAYMENT_GATEWAY_UNAVAILABLE", cause);
    }
}
