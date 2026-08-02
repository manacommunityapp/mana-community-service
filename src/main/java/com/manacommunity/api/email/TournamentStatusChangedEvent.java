package com.manacommunity.api.email;

import com.manacommunity.api.model.Tournament;

/**
 * Published after a tournament's status is committed to the DB.
 * Consumed by {@link TournamentEmailEventListener} to fire bulk emails
 * off the HTTP thread, after the transaction is guaranteed committed.
 */
public record TournamentStatusChangedEvent(Long tournamentId, Tournament.EventStatus newStatus) {}
