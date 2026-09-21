package com.manacommunity.api.email;

import com.manacommunity.api.model.SportsEventStatus;

/**
 * Published after a tournament's status is committed to the DB.
 * Consumed by {@link SportsTournamentEmailEventListener} to fire bulk emails
 * off the HTTP thread, after the transaction is guaranteed committed.
 */
public record SportsTournamentStatusChangedEvent(Long tournamentId, SportsEventStatus newStatus) {}
