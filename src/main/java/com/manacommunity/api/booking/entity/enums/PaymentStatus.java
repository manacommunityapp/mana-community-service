package com.manacommunity.api.booking.entity.enums;

public enum PaymentStatus {
    /** Payment order created, PG not yet launched. */
    INITIATED,
    /** PG launched or awaiting admin confirmation. */
    PENDING,
    /** Gateway confirmed payment capture. */
    PAID,
    /** Refund initiated, awaiting gateway confirmation. */
    REFUND_PENDING,
    /** Full or partial refund confirmed. */
    REFUNDED,
    PARTIALLY_REFUNDED,
    /** No payment collected. */
    WAIVED
}
