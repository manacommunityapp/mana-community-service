package com.manacommunity.api.email;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles sending general tournament announcement emails (e.g. registration open announcements).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentEmailService {

    private final EmailSupport support;
    private final EmailTemplateRenderer renderer;
    private final EmailService emailService;
    private final AppUserRepository appUserRepository;

    public void sendTournamentRegistrationOpen(Tournament tournament) {
        if (tournament == null) return;

        // 1. Identify all active members in the tournament's community
        List<AppUser> recipients;
        if (tournament.getCommunity() != null && tournament.getCommunity().getId() != null) {
            recipients = appUserRepository.findByCommunityIdAndIsActiveTrue(tournament.getCommunity().getId());
        } else {
            // Global tournament: notify all active users
            recipients = appUserRepository.findAll().stream()
                    .filter(u -> u != null && Boolean.TRUE.equals(u.getIsActive()))
                    .collect(Collectors.toList());
        }

        if (recipients == null || recipients.isEmpty()) {
            log.info("No active recipients found to notify about tournament '{}' registration open", tournament.getName());
            return;
        }

        // 2. Resolve sports name list
        String sportsList = "";
        if (tournament.getSportsEvents() != null && !tournament.getSportsEvents().isEmpty()) {
            sportsList = tournament.getSportsEvents().stream()
                    .filter(e -> e.getSport() != null)
                    .map(e -> e.getSport().getName())
                    .distinct()
                    .collect(Collectors.joining(", "));
        }

        // 3. Resolve venue
        String venueName = "TBA";
        if (tournament.getSportsEvents() != null && !tournament.getSportsEvents().isEmpty()) {
            venueName = tournament.getSportsEvents().stream()
                    .filter(e -> e.getVenue() != null)
                    .map(e -> e.getVenue().getName())
                    .findFirst()
                    .orElse("TBA");
        }

        // 4. Resolve contact information
        String contactDetails = "";
        if (!support.isBlank(tournament.getContactName())) {
            contactDetails = tournament.getContactName();
            if (!support.isBlank(tournament.getContactEmail())) {
                contactDetails += " (" + tournament.getContactEmail() + ")";
            }
            if (!support.isBlank(tournament.getContactNumber())) {
                contactDetails += " - " + tournament.getContactNumber();
            }
        }

        // 5. Build dates formatting
        String registrationPeriod = support.formatDate(tournament.getRegistrationDateStart()) + " to " + support.formatDate(tournament.getRegistrationDateEnd());
        String eventDates = support.formatDate(tournament.getEventDateStart()) + " to " + support.formatDate(tournament.getEventDateEnd());

        List<EmailMessage> batch = new ArrayList<>();
        for (AppUser user : recipients) {
            if (user == null || support.isBlank(user.getEmail())) continue;
            try {
                Map<String, Object> vars = support.baseVars(user.getFullName());
                vars.put("tournamentName", tournament.getName());
                vars.put("description", tournament.getDescription());
                vars.put("sportName", sportsList);
                vars.put("registrationPeriod", registrationPeriod);
                vars.put("eventDates", eventDates);
                vars.put("venueName", venueName);
                vars.put("contactDetails", contactDetails);
                vars.put("actionUrl", support.props().getBaseUrl() + "/sports");

                String subject = EmailTemplate.REGISTRATION_OPEN.defaultSubject() + " — " + tournament.getName();
                String html = renderer.render(EmailTemplate.REGISTRATION_OPEN, vars);
                batch.add(new EmailMessage(user.getEmail(), user.getFullName(), subject, html));
            } catch (Exception e) {
                log.error("Failed to build 'registration open' email for user {}", user.getId(), e);
            }
        }

        emailService.sendAll(batch);
        log.info("Queued {} 'registration open' emails for tournament '{}'", batch.size(), tournament.getName());
    }
}
