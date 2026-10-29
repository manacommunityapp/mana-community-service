package com.manacommunity.api.transaction.enums;

public enum TransactionStatus {
    INITIATED,
    AUTHORIZED,
    CAPTURED,
    IN_ESCROW,
    SETTLED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    FAILED,
    DISPUTED
}
