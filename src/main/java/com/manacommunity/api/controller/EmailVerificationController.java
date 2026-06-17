package com.manacommunity.api.controller;

import com.manacommunity.api.email.EmailMessage;
import com.manacommunity.api.email.EmailProperties;
import com.manacommunity.api.email.EmailService;
import com.manacommunity.api.email.EmailTemplate;
import com.manacommunity.api.email.EmailTemplateRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Developer/admin tooling to <strong>verify</strong> the email templates without
 * needing a live registration or a configured SMTP server:
 *
 * <ul>
 *   <li>{@code GET  /api/admin/email/templates}            — list the available templates</li>
 *   <li>{@code GET  /api/admin/email/preview/{template}}    — render a template to HTML (open in a browser)</li>
 *   <li>{@code POST /api/admin/email/test?to=&template=}    — render + dispatch a sample to a real address</li>
 * </ul>
 *
 * Previews use representative sample data so the markup can be reviewed visually.
 */
@RestController
@RequestMapping("/api/admin/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailTemplateRenderer renderer;
    private final EmailService          emailService;
    private final EmailProperties       props;

    @GetMapping("/templates")
    public ResponseEntity<Map<String, String>> templates() {
        Map<String, String> out = new LinkedHashMap<>();
        for (EmailTemplate t : EmailTemplate.values()) {
            out.put(t.name(), t.defaultSubject());
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping(value = "/preview/{template}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> preview(@PathVariable("template") EmailTemplate template) {
        String html = renderer.render(template, sampleVars(template));
        return ResponseEntity.ok(html);
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> sendTest(@RequestParam("to") String to,
                                                        @RequestParam("template") EmailTemplate template) {
        String html = renderer.render(template, sampleVars(template));
        emailService.send(new EmailMessage(to, "Test Recipient",
                "[TEST] " + template.defaultSubject(), html));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("template", template.name());
        resp.put("to", to);
        resp.put("mailEnabled", props.isEnabled());
        resp.put("note", props.isEnabled()
                ? "Dispatched via SMTP."
                : "app.mail.enabled=false — email was rendered + logged but not actually sent.");
        return ResponseEntity.ok(resp);
    }

    // ── Sample data per template ──────────────────────────────────────

    private Map<String, Object> sampleVars(EmailTemplate template) {
        Map<String, Object> v = new HashMap<>();
        v.put("appName", props.getFromName());
        v.put("baseUrl", props.getBaseUrl());
        v.put("recipientName", "Rahul Sharma");
        v.put("year", LocalDate.now().getYear());
        v.put("actionUrl", props.getBaseUrl() + "/profile");

        switch (template) {
            case REGISTRATION_RECEIVED -> {
                v.put("eventName", "Summer Smash Badminton 2026");
                v.put("sportName", "Badminton");
                v.put("categoryName", "Men's Open");
                v.put("matchType", "Singles");
                v.put("eventDate", "Sat, 20 Jun 2026");
                v.put("venueName", "Community Sports Arena");
                v.put("status", "Registered");
            }
            case REGISTRATION_CONFIRMED -> {
                v.put("eventName", "Summer Smash Badminton 2026");
                v.put("sportName", "Badminton");
                v.put("categoryName", "Men's Open");
                v.put("eventDate", "Sat, 20 Jun 2026");
                v.put("venueName", "Community Sports Arena");
                v.put("confirmedAt", "Sat, 14 Jun 2026 10:30 AM");
            }
            case REGISTRATION_REJECTED -> {
                v.put("eventName", "Summer Smash Badminton 2026");
                v.put("sportName", "Badminton");
                v.put("categoryName", "Men's Open");
                v.put("eventDate", "Sat, 20 Jun 2026");
                v.put("reason", "The Men's Open category reached its maximum number of participants.");
            }
            case SCHEDULE_PUBLISHED -> {
                v.put("tournamentName", "Summer Smash Cup");
                v.put("eventName", "Summer Smash Badminton 2026");
                v.put("sportName", "Badminton");
                v.put("startDate", "Sat, 20 Jun 2026");
                v.put("venueName", "Community Sports Arena");
                v.put("matchCount", 24);
            }
            case MATCH_REMINDER -> {
                v.put("tournamentName", "Summer Smash Cup");
                v.put("roundName", "Quarter Final");
                v.put("homeTeam", "Rahul Sharma");
                v.put("awayTeam", "Amit Kumar");
                v.put("matchDate", "Sat, 20 Jun 2026");
                v.put("matchTime", "09:40 AM");
                v.put("venueName", "Community Sports Arena");
                v.put("courtName", "Court 2");
                v.put("minutesUntilStart", 30);
            }
            case WINNER_NOTIFICATION -> {
                v.put("tournamentName", "Summer Smash Cup");
                v.put("roundName", "Quarter Final");
                v.put("opponentName", "Amit Kumar");
                v.put("score", "21-18, 21-15");
                v.put("nextRoundInfo", "You play the winner of QF-2 in the Semi-Final on Sun, 21 Jun.");
            }
            case TOURNAMENT_COMPLETION -> {
                v.put("tournamentName", "Summer Smash Cup");
                v.put("championName", "Rahul Sharma");
                v.put("runnerUpName", "Amit Kumar");
                v.put("thirdPlaceName", "Vikram Singh");
            }
            case PRIZE_DISTRIBUTION -> {
                v.put("tournamentName", "Summer Smash Cup");
                v.put("position", "Champion");
                v.put("prize", "₹10,000 + Trophy");
                v.put("ceremonyDate", "Sun, 21 Jun 2026, 06:00 PM");
                v.put("venueName", "Community Sports Arena");
            }
            case EMAIL_OTP -> {
                v.put("otpCode", "428913");
                v.put("expiryMinutes", 10);
            }
        }
        return v;
    }
}
