package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.PoojaReserveRequest;
import com.manacommunity.api.events.dto.PoojaReserveResponse;
import com.manacommunity.api.user.model.AppUser;

public interface PoojaSlotReservationService {

    /**
     * Acquires a PESSIMISTIC_WRITE lock on the schedule row, expires stale reservations,
     * checks capacity, and creates a new reservation — all in one transaction.
     */
    PoojaReserveResponse reserve(Long scheduleId, PoojaReserveRequest request, AppUser user);

    /** Confirms the reservation once the registration is saved (links registration ID). */
    void confirmReservation(Long reservationId, Long registrationId);

    /** Releases the hold (marks CANCELLED) when a user cancels their registration. */
    void releaseReservation(Long reservationId);
}
