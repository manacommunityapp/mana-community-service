package com.manacommunity.api.events.enums;

public enum ReservationStatus {
    /** Slot locked — waiting for user to complete booking. */
    RESERVED,
    /** Slot locked — payment initiated but not yet captured. */
    PAYMENT_PENDING,
    /** Booking confirmed, capacity permanently deducted. */
    CONFIRMED,
    /** expires_at passed before confirmation. Capacity released. */
    EXPIRED,
    /** Cancelled by user or admin. Capacity released. */
    CANCELLED
}
