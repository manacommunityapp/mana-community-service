package com.manacommunity.api.events.enums;

/**
 * Admin-controlled persisted state of a PoojaSchedule slot.
 * OPEN / LIMITED / FULL are computed dynamically from availability;
 * only BLOCKED and CLOSED are stored in the DB.
 */
public enum PoojaScheduleStatus {
    /** Default — accepting bookings. */
    OPEN,
    /** Computed: < 20 % family capacity remaining. */
    LIMITED,
    /** Computed: zero family or devotee capacity remaining. */
    FULL,
    /** Admin-blocked — not accepting bookings. */
    BLOCKED,
    /** Admin-closed — registration window ended. */
    CLOSED
}
