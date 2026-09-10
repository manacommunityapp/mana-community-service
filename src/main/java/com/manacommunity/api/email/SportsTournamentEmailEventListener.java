package com.manacommunity.api.email;

import com.manacommunity.api.model.SportsTournament;
import com.manacommunity.api.repository.SportsTournamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Fires bulk tournament emails after the triggering transaction commits,
 * on a background thread — so the HTTP response returns to the admin UI
 * immediately, regardless of how many recipients the community has.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SportsTournamentEmailEventListener {

    private final SportsTournamentEmailService tournamentEmailService;
    private final SportsTournamentRepository tournamentRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(SportsTournamentStatusChangedEvent event) {
        if (event.newStatus() != SportsTournament.EventStatus.REGISTRATION_OPEN) return;

        tournamentRepository.findById(event.tournamentId()).ifPresentOrElse(
                tournament -> {
                    log.info("Sending registration-open emails for tournament {} ({})",
                            tournament.getId(), tournament.getName());
                    try {
                        tournamentEmailService.sendTournamentRegistrationOpen(tournament);
                    } catch (Exception e) {
                        log.error("Failed to send registration-open emails for tournament {}: {}",
                                event.tournamentId(), e.getMessage(), e);
                    }
                },
                () -> log.warn("SportsTournament {} not found — registration-open emails skipped", event.tournamentId())
        );
    }
}
