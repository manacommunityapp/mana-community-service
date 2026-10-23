package com.manacommunity.api.events.enums;

public enum PoojaSevaStatus {
    /** Default — pooja is open for registrations. */
    ACTIVE,
    /** Temporarily halted — new registrations blocked; existing ones untouched. */
    PAUSED,
    /** Permanently cancelled — all active registrations are cascade-cancelled. */
    CANCELLED,
    /** Pooja concluded — read-only, no new registrations. */
    COMPLETED,
    /** Done and hidden from active lists; data retained. */
    ARCHIVED
}
