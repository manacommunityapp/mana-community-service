package com.manacommunity.api.commerce.core.model;

public enum CommerceOrderStatus {
    PENDING,
    PAYMENT_AUTHORIZED,
    CONFIRMED,
    PROCESSING,
    ALLOCATED,
    PICKED,
    READY_FOR_PICKUP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    COMPLETED,
    CANCELLED,
    REFUNDED,
    DISPUTED
}