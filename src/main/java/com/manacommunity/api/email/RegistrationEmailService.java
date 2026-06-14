package com.manacommunity.api.email;

import com.manacommunity.api.model.SportsEvent;
import com.manacommunity.api.model.SportsEventRegistration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * The single, dedicated email service for the registration process. Both
 * registration steps go through one entry point — {@link #send(SportsEventRegistration, Stage)} —
 * which renders the right template for the {@link Stage}:
 *
 * <ul>
 *   <li>{@link Stage#RECEIVED} — a player submitted a sports registration (status REGISTERED)</li>
 *   <li>{@link Stage#CONFIRMED} — the organiser confirmed it (status CONFIRMED)</li>
 *   <li>{@link Stage#REJECTED} — the organiser did not approve it (optional reason)</li>
 * </ul>
 *
 * Failures are swallowed (logged) so a notification can never break the
 * surrounding registration transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationEmailService {

    /** Which point in the registration lifecycle the email represents. */
    public enum Stage { RECEIVED, CONFIRMED, REJECTED }

    private final EmailSupport          support;
    private final EmailTemplateRenderer renderer;
    private final EmailService          emailService;

    public void send(SportsEventRegistration reg, Stage stage) {
        send(reg, stage, null);
    }

    /** {@code reason} is only used by {@link Stage#REJECTED}; ignored otherwise. */
    public void send(SportsEventRegistration reg, Stage stage, String reason) {
        if (reg == null || stage == null) return;
        try {
            String to = recipientEmail(reg);
            if (to == null) {
                log.debug("[EMAIL] No address for registration {} — skipping {}", reg.getId(), stage);
                return;
            }

            String name = playerName(reg);
            SportsEvent event = reg.getEvent();

            // Variables shared by both registration templates.
            Map<String, Object> vars = support.baseVars(name);
            vars.put("eventName", event != null ? event.getName() : "the event");
            vars.put("sportName", event != null && event.getSport() != null ? event.getSport().getName() : "");
            vars.put("categoryName", reg.getCategory() != null ? reg.getCategory().getName() : "");
            vars.put("eventDate", event != null ? support.formatDate(event.getEventDateStart()) : "TBA");
            vars.put("venueName", event != null && event.getVenue() != null ? event.getVenue().getName() : "TBA");
            vars.put("actionUrl", support.props().getBaseUrl() + "/profile");

            EmailTemplate template;
            String subject;
            switch (stage) {
                case RECEIVED -> {
                    vars.put("matchType", reg.getMatchType() != null ? support.prettify(reg.getMatchType().name()) : "");
                    vars.put("status", reg.getStatus() != null ? support.prettify(reg.getStatus().name()) : "Registered");
                    template = EmailTemplate.REGISTRATION_RECEIVED;
                    subject = template.defaultSubject() + (event != null ? " — " + event.getName() : "");
                }
                case CONFIRMED -> {
                    vars.put("confirmedAt", support.formatDateTime(LocalDateTime.now()));
                    template = EmailTemplate.REGISTRATION_CONFIRMED;
                    subject = template.defaultSubject() + (event != null ? " " + event.getName() : "");
                }
                case REJECTED -> {
                    vars.put("reason", support.isBlank(reason) ? "" : reason.trim());
                    template = EmailTemplate.REGISTRATION_REJECTED;
                    subject = template.defaultSubject() + (event != null ? " — " + event.getName() : "");
                }
                default -> {
                    return;
                }
            }

            String html = renderer.render(template, vars);
            emailService.send(new EmailMessage(to, name, subject, html));
        } catch (Exception e) {
            log.error("Failed to build registration ({}) email for reg {}", stage, reg.getId(), e);
        }
    }

    /** Prefer the registration's own email, fall back to the linked user's. */
    private String recipientEmail(SportsEventRegistration reg) {
        if (!support.isBlank(reg.getEmail())) return reg.getEmail().trim();
        if (reg.getUser() != null && !support.isBlank(reg.getUser().getEmail())) return reg.getUser().getEmail().trim();
        return null;
    }

    private String playerName(SportsEventRegistration reg) {
        if (!support.isBlank(reg.getPlayerName())) return reg.getPlayerName();
        if (reg.getUser() != null) return reg.getUser().getFullName();
        return "there";
    }
}
