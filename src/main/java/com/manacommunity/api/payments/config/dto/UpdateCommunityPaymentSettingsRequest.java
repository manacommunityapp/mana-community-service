package com.manacommunity.api.payments.config.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateCommunityPaymentSettingsRequest {
    /** Gateways this community wants to allow (must be subset of platform-enabled). */
    private List<String> allowedPaymentGateways;
    /** Community-level default gateway for pre-selection. */
    private String defaultGateway;
    private Boolean manualPaymentEnabled;
    private List<String> manualPaymentModes;
    /** Cashier-facing instruction text shown at checkout. */
    private String paymentInstructions;
    private Boolean cashierCanRecordPayment;
    /** Optional fallback gateway if primary is unavailable. */
    private String fallbackGateway;
}
