package com.manacommunity.api.events.enums;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Lifecycle states for a Pooja booking (event_pooja_user_registrations.status).
 *
 * The embedded transition table is the single source of truth for valid moves.
 * Use {@link #canTransitionTo} to guard status updates and {@link #isReschedulable}
 * to guard reschedule operations.
 *
 * Terminal states (COMPLETED, CANCELLED, EXPIRED, NO_SHOW) accept no further transitions.
 */
public enum PoojaRegistrationStatus {

    /** Slot locked — user has not yet completed booking (pre-payment hold). */
    RESERVED,

    /** Payment initiated but not yet captured / confirmed by gateway. */
    PAYMENT_PENDING,

    /** Booking fully confirmed; slot capacity permanently deducted. */
    CONFIRMED,

    /** Devotee has checked in at the venue. Rescheduling is no longer permitted. */
    CHECKED_IN,

    /** Pooja seva is actively in progress for this booking. */
    IN_PROGRESS,

    /** Seva completed successfully. Terminal state. */
    COMPLETED,

    /** Booking cancelled by user or admin. Capacity released. Terminal state. */
    CANCELLED,

    /** Reservation window elapsed before confirmation. Terminal state. */
    EXPIRED,

    /** Devotee did not show up. Terminal state. */
    NO_SHOW;

    // ── State machine ────────────────────────────────────────────────────────

    private static final Map<PoojaRegistrationStatus, Set<PoojaRegistrationStatus>> TRANSITIONS;

    static {
        TRANSITIONS = new EnumMap<>(PoojaRegistrationStatus.class);
        TRANSITIONS.put(RESERVED,        EnumSet.of(PAYMENT_PENDING, CONFIRMED, CANCELLED, EXPIRED));
        TRANSITIONS.put(PAYMENT_PENDING, EnumSet.of(CONFIRMED, CANCELLED, EXPIRED));
        TRANSITIONS.put(CONFIRMED,       EnumSet.of(CHECKED_IN, CANCELLED, NO_SHOW));
        TRANSITIONS.put(CHECKED_IN,      EnumSet.of(IN_PROGRESS, CANCELLED, NO_SHOW));
        TRANSITIONS.put(IN_PROGRESS,     EnumSet.of(COMPLETED, CANCELLED, NO_SHOW));
        TRANSITIONS.put(COMPLETED,       EnumSet.noneOf(PoojaRegistrationStatus.class));
        TRANSITIONS.put(CANCELLED,       EnumSet.noneOf(PoojaRegistrationStatus.class));
        TRANSITIONS.put(EXPIRED,         EnumSet.noneOf(PoojaRegistrationStatus.class));
        TRANSITIONS.put(NO_SHOW,         EnumSet.noneOf(PoojaRegistrationStatus.class));
    }

    /** Returns true if moving from this state to {@code next} is a valid transition. */
    public boolean canTransitionTo(PoojaRegistrationStatus next) {
        return TRANSITIONS.getOrDefault(this, EnumSet.noneOf(PoojaRegistrationStatus.class))
                          .contains(next);
    }

    /**
     * Returns true when rescheduling is permitted from this state.
     * Only CONFIRMED bookings may be rescheduled — once a devotee has checked in
     * or the seva is in progress, rescheduling is no longer meaningful.
     */
    public boolean isReschedulable() {
        return this == CONFIRMED;
    }

    /** Returns true for states that permanently close the booking (no further transitions). */
    public boolean isTerminal() {
        return TRANSITIONS.getOrDefault(this, EnumSet.noneOf(PoojaRegistrationStatus.class)).isEmpty();
    }

    /**
     * Parses a raw status string from the DB / request, falling back to {@code fallback}
     * when the value is null, blank, or unrecognised.
     */
    public static PoojaRegistrationStatus parse(String value, PoojaRegistrationStatus fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
