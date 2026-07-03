package com.manacommunity.api.email;

import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The end-of-tournament emails: a player won their match, the tournament finished,
 * and the prize/closing details. Grouped together because they share the same
 * tournament-result context and fire late in the flow.
 *
 * <p>These take plain result fields (winner, positions, prize) rather than result
 * entities, so they can be driven from wherever the result is finalised. Every
 * method swallows its own failures so a notification never breaks the caller.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentResultEmailService {

    private final EmailSupport          support;
    private final EmailTemplateRenderer renderer;
    private final EmailService          emailService;

    // ── Winner notification (a round completed) ───────────────────────

    /**
     * Tells {@code winner} they won their match and advanced.
     *
     * @param nextRoundInfo optional "you play X in the Semi-Final" line (may be null)
     */
    public void sendWinnerNotification(AppUser winner, String tournamentName, String roundName,
                                       String opponentName, String score, String nextRoundInfo) {
        if (winner == null || support.isBlank(winner.getEmail())) return;
        try {
            Map<String, Object> vars = support.baseVars(winner.getFullName());
            vars.put("tournamentName", orDefault(tournamentName, "the tournament"));
            vars.put("roundName", orDefault(roundName, "your match"));
            vars.put("opponentName", orDefault(opponentName, "your opponent"));
            vars.put("score", orDefault(score, ""));
            vars.put("nextRoundInfo", orDefault(nextRoundInfo, ""));
            vars.put("actionUrl", support.props().getBaseUrl() + "/profile?tab=schedule");

            dispatch(EmailTemplate.WINNER_NOTIFICATION, winner.getEmail(), winner.getFullName(),
                    EmailTemplate.WINNER_NOTIFICATION.defaultSubject() + " — " + orDefault(roundName, ""),
                    vars);
        } catch (Exception e) {
            log.error("Failed to build winner-notification email for user {}", winner.getId(), e);
        }
    }

    // ── Tournament completion (final results) ─────────────────────────

    /** Sends the final standings to all participants. */
    public void sendTournamentCompletion(List<AppUser> recipients, String tournamentName,
                                         String championName, String runnerUpName, String thirdPlaceName) {
        if (recipients == null || recipients.isEmpty()) return;

        List<EmailMessage> batch = new ArrayList<>();
        for (AppUser user : recipients) {
            if (user == null || support.isBlank(user.getEmail())) continue;
            try {
                Map<String, Object> vars = support.baseVars(user.getFullName());
                vars.put("tournamentName", orDefault(tournamentName, "the tournament"));
                vars.put("championName", orDefault(championName, "TBA"));
                vars.put("runnerUpName", orDefault(runnerUpName, "TBA"));
                vars.put("thirdPlaceName", orDefault(thirdPlaceName, ""));
                vars.put("actionUrl", support.props().getBaseUrl() + "/profile?tab=results");

                String subject = EmailTemplate.TOURNAMENT_COMPLETION.defaultSubject()
                        + " — " + orDefault(tournamentName, "");
                String html = renderer.render(EmailTemplate.TOURNAMENT_COMPLETION, vars);
                batch.add(new EmailMessage(user.getEmail(), user.getFullName(), subject, html));
            } catch (Exception e) {
                log.error("Failed to build tournament-completion email for user {}", user.getId(), e);
            }
        }
        emailService.sendAll(batch);
        log.info("Queued {} tournament-completion emails for '{}'", batch.size(), tournamentName);
    }

    // ── Prize distribution (event closure) ────────────────────────────

    /**
     * Sends a winner their prize / closing-ceremony details.
     *
     * @param position    e.g. "Champion", "Runner-Up", "3rd Place"
     * @param prize       e.g. "₹10,000 + Trophy"
     * @param ceremonyDate when/where the prize is handed out (may be null)
     */
    public void sendPrizeDistribution(AppUser recipient, String tournamentName, String position,
                                      String prize, String ceremonyDate, String venueName) {
        if (recipient == null || support.isBlank(recipient.getEmail())) return;
        try {
            Map<String, Object> vars = support.baseVars(recipient.getFullName());
            vars.put("tournamentName", orDefault(tournamentName, "the tournament"));
            vars.put("position", orDefault(position, "Winner"));
            vars.put("prize", orDefault(prize, ""));
            vars.put("ceremonyDate", orDefault(ceremonyDate, "TBA"));
            vars.put("venueName", orDefault(venueName, "TBA"));
            vars.put("actionUrl", support.props().getBaseUrl() + "/profile?tab=results");

            dispatch(EmailTemplate.PRIZE_DISTRIBUTION, recipient.getEmail(), recipient.getFullName(),
                    EmailTemplate.PRIZE_DISTRIBUTION.defaultSubject() + " — " + orDefault(tournamentName, ""),
                    vars);
        } catch (Exception e) {
            log.error("Failed to build prize-distribution email for user {}", recipient.getId(), e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private void dispatch(EmailTemplate template, String to, String toName,
                          String subject, Map<String, Object> vars) {
        String html = renderer.render(template, vars);
        emailService.send(new EmailMessage(to, toName, subject, html));
    }

    private String orDefault(String value, String fallback) {
        return support.isBlank(value) ? fallback : value.trim();
    }
}
