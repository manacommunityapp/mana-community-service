package com.manacommunity.api.email;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.scheduler.TournamentMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Emails a participant a "your match starts soon" reminder shortly before kickoff.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchReminderEmailService {

    private final EmailSupport          support;
    private final EmailTemplateRenderer renderer;
    private final EmailService          emailService;

    public void sendMatchReminder(TournamentMatch match, List<AppUser> recipients, long minutesUntilStart) {
        if (match == null || recipients == null || recipients.isEmpty()) return;

        String homeTeam = match.getTeamA() != null ? match.getTeamA().getTeamName() : "TBD";
        String awayTeam = match.getTeamB() != null ? match.getTeamB().getTeamName() : "TBD";
        String tournamentName = match.getConfig() != null ? match.getConfig().getTournamentName() : "the tournament";
        String roundName = match.getRound() != null ? support.prettify(match.getRound().name()) : "Match";
        String venueName = match.getVenue() != null ? match.getVenue().getName() : "TBA";
        String courtName = match.getCourt() != null ? match.getCourt().getName() : "";

        List<EmailMessage> batch = new ArrayList<>();
        for (AppUser user : recipients) {
            if (user == null || support.isBlank(user.getEmail())) continue;
            try {
                Map<String, Object> vars = support.baseVars(user.getFullName());
                vars.put("tournamentName", tournamentName);
                vars.put("roundName", roundName);
                vars.put("homeTeam", homeTeam);
                vars.put("awayTeam", awayTeam);
                vars.put("matchDate", support.formatDate(match.getScheduledAt() != null
                        ? match.getScheduledAt().toLocalDate() : null));
                vars.put("matchTime", support.formatTime(match.getScheduledAt()));
                vars.put("venueName", venueName);
                vars.put("courtName", courtName);
                vars.put("minutesUntilStart", minutesUntilStart);
                vars.put("actionUrl", support.props().getBaseUrl() + "/profile?tab=schedule");

                String subject = EmailTemplate.MATCH_REMINDER.defaultSubject() + " — " + roundName;
                String html = renderer.render(EmailTemplate.MATCH_REMINDER, vars);
                batch.add(new EmailMessage(user.getEmail(), user.getFullName(), subject, html));
            } catch (Exception e) {
                log.error("Failed to build 'match reminder' email for user {}", user.getId(), e);
            }
        }
        emailService.sendAll(batch);
        log.info("Queued {} 'match reminder' emails for match {} ({})",
                batch.size(), match.getId(), roundName);
    }
}
