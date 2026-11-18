package com.manacommunity.api.payments.config.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdatePlatformPaymentConfigRequest {
    private Boolean cashfreeEnabled;
    private Boolean razorpayEnabled;
    private Boolean manaPayEnabled;
    private Boolean bbpsEnabled;
    private String defaultGateway;
    /** Convenience fee percent — NUMERIC, never float. */
    private BigDecimal convenienceFeePercent;
    private BigDecimal maxTransactionAmount;
}
