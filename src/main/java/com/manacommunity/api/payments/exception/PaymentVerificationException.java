package com.manacommunity.api.payments.exception;

import com.manacommunity.api.exception.ManaCommunityException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when Razorpay payment signature verification fails.
 * This indicates a tampered or replayed callback from the frontend.
 */
public class PaymentVerificationException extends ManaCommunityException {

    public PaymentVerificationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "PAYMENT_VERIFICATION_FAILED");
    }
}
