package com.manacommunity.api.transactioncore.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * Published after a gateway payment (Cashfree or Razorpay) is marked PAID.
 * Consumed by PaymentLedgerPostingListener to post the double-entry journal.
 */
@Getter
@RequiredArgsConstructor
public class GatewayPaymentCapturedEvent {

    public enum Gateway { CASHFREE, RAZORPAY, MANUAL, MANA_PAY }

    private final Gateway gateway;
    private final String  gatewayOrderId;
    private final String  gatewayPaymentId;
    private final BigDecimal amount;
    private final String  currency;
    private final String  narration;
    /** Internal order DB id — used as sourceDocumentId in the journal entry. */
    private final Long    internalOrderId;
}
