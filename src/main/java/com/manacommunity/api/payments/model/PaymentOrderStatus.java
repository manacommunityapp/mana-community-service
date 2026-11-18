package com.manacommunity.api.payments.model;

public enum PaymentOrderStatus {
    INITIATED, PENDING, SUCCESS, FAILED,
    REFUND_PENDING, REFUNDED, PARTIALLY_REFUNDED, VOIDED, EXPIRED;

    public boolean isTerminal() {
        return this == FAILED || this == VOIDED || this == EXPIRED || this == REFUNDED;
    }

    public boolean isRefundable() {
        return this == SUCCESS;
    }
}
