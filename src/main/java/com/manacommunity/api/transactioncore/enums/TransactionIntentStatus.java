package com.manacommunity.api.transactioncore.enums;

public enum TransactionIntentStatus {
    INITIATED,
    PROCESSING,
    ESCROW_HELD,
    CAPTURED,
    SETTLED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    FAILED,
    DISPUTED,
    CANCELLED
}
