package com.manacommunity.api.payments.config.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutOptionsResponse {

    /** Gateways available for this community at checkout, e.g. ["CASHFREE", "MANUAL"]. */
    private List<String> gatewayOptions;

    /** Effective default gateway for pre-selection in the UI. */
    private String defaultGateway;

    /** Whether community admin/cashier can record an offline payment. */
    private boolean manualPaymentEnabled;

    /** Modes available for manual recording — empty if manualPaymentEnabled is false. */
    private List<String> manualPaymentModes;

    /** Community-specific instructions shown at checkout (nullable). */
    private String paymentInstructions;

    /** Platform-level convenience fee percent (0 if none). Never float. */
    private BigDecimal convenienceFeePercent;

    /** Max single transaction amount — null means no limit. */
    private BigDecimal maxTransactionAmount;
}
