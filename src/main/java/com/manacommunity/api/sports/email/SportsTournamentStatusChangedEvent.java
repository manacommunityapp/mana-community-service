package com.manacommunity.api.sports.email;
import com.manacommunity.api.email.EmailSupport;
import com.manacommunity.api.email.EmailTemplateRenderer;
import com.manacommunity.api.email.EmailService;
import com.manacommunity.api.email.EmailTemplate;

import com.manacommunity.api.sports.model.SportsEventStatus;

/**
 * Published after a tournament's status is committed to the DB.
 * Consumed by {@link SportsTournamentEmailEventListener} to fire bulk emails
 * off the HTTP thread, after the transaction is guaranteed committed.
 */
public record SportsTournamentStatusChangedEvent(Long tournamentId, SportsEventStatus newStatus) {}
