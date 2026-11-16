package com.manacommunity.api.freshkitchen.model;

public enum BatchStatus {
    PLANNED,
    IN_PRODUCTION,
    QUALITY_CHECK,
    RELEASED,
    REJECTED,
    EXPIRED,
    LOCKED_OUT,
    QUARANTINED
}
