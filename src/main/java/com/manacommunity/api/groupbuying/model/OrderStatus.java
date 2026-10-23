package com.manacommunity.api.groupbuying.model;

public enum OrderStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    PREPARING,
    OUT_FOR_DELIVERY,
    READY_FOR_PICKUP,
    DELIVERED,
    PICKED_UP,
    CANCELLED,
    REFUNDED,
    DISPUTED
}
