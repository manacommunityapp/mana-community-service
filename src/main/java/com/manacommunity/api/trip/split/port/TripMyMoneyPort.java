package com.manacommunity.api.trip.split.port;

import java.time.LocalDate;

/**
 * What Trip Split needs from My Money. The trips package depends only on this interface; the
 * personal-finance package implements it, so trip code never touches personal-finance internals.
 * Every call is idempotent on (userId, sourceType, sourceId).
 */
public interface TripMyMoneyPort {

    /** Create or update the user's personal transaction for this source. */
    void upsert(Long userId, String sourceType, String sourceId, boolean income, long amountPaise,
                LocalDate date, String label);

    /** Remove the user's personal transaction for this source, if there is one. */
    void remove(Long userId, String sourceType, String sourceId);
}
