package com.manacommunity.api.controller;

import com.manacommunity.api.email.EmailMessage;
import com.manacommunity.api.email.EmailProperties;
import com.manacommunity.api.email.EmailService;
import com.manacommunity.api.email.EmailTemplate;
import com.manacommunity.api.email.EmailTemplateRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Admin tooling to verify, preview and test-fire every email template
 * without needing a live registration, match or tournament.
 *
 * <ul>
 *   <li>{@code GET  /templates}                  — list all templates + subjects</li>
 *   <li>{@code GET  /preview/{template}}          — render a template to HTML</li>
 *   <li>{@code GET  /sample-payloads}             — sample payload for every template</li>
 *   <li>{@code GET  /sample-payloads/{template}}  — sample payload for one template</li>
 *   <li>{@code GET  /health}                      — email config status</li>
 *   <li>{@code POST /test}                        — send one template (with optional custom vars)</li>
 *   <li>{@code POST /test-all}                    — send ALL templates to one address</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailTemplateRenderer renderer;
    private final EmailService          emailService;
    private final EmailProperties       props;

    // Deliberately simple RFC-5322-ish check — good enough to reject typos and
    // header-injection attempts (CR/LF, missing @) without a full grammar parser.
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    // Test-send endpoints dispatch real emails, so they get a much tighter,
    // per-admin limit than the global API rate limiter — capped low enough to
    // stop accidental/careless repeated firing without blocking normal QA use.
    // /test-all fans out to every template in one call, so it gets its own,
    // stricter budget rather than sharing /test's counter.
    private static final int TEST_SEND_LIMIT_PER_MINUTE = 10;
    private static final int TEST_SEND_ALL_LIMIT_PER_MINUTE = 2;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000L;
    private final ConcurrentHashMap<String, RateLimitWindow> rateLimitWindows = new ConcurrentHashMap<>();

    // ── List templates ───────────────────────────────────────────────

    @GetMapping("/templates")
    public ResponseEntity<Map<String, Object>> templates() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (EmailTemplate t : EmailTemplate.values()) {
            EmailTemplate.Trigger trigger = t.trigger();
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("key", t.name());
            entry.put("subject", t.defaultSubject());
            entry.put("templateFile", t.templateName() + ".html");
            entry.put("category", t.category().name());
            entry.put("triggerMenuPath", trigger.menuPath());
            entry.put("triggerWired", trigger.wired());
            entry.put("triggerDescription", trigger.description());
            list.add(entry);
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("count", list.size());
        resp.put("templates", list);
        return ResponseEntity.ok(resp);
    }

    // ── Preview (HTML) ───────────────────────────────────────────────

    @GetMapping(value = "/preview/{template}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> preview(@PathVariable("template") EmailTemplate template) {
        return ResponseEntity.ok(renderer.render(template, sampleVars(template)));
    }

    @PostMapping(value = "/preview/{template}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> previewPost(
            @PathVariable("template") EmailTemplate template,
            @RequestBody(required = false) Map<String, Object> customVars) {
        Map<String, Object> vars = sampleVars(template);
        if (customVars != null) {
            vars.putAll(customVars);
        }
        return ResponseEntity.ok(renderer.render(template, vars));
    }

    // ── Sample payloads ──────────────────────────────────────────────

    @GetMapping("/sample-payloads")
    public ResponseEntity<Map<String, Object>> allSamplePayloads() {
        Map<String, Object> payloads = new LinkedHashMap<>();
        for (EmailTemplate t : EmailTemplate.values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("subject", t.defaultSubject());
            entry.put("variables", sampleVars(t));
            payloads.put(t.name(), entry);
        }
        return ResponseEntity.ok(payloads);
    }

    @GetMapping("/sample-payloads/{template}")
    public ResponseEntity<Map<String, Object>> samplePayload(
            @PathVariable("template") EmailTemplate template) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("template", template.name());
        resp.put("subject", template.defaultSubject());
        resp.put("variables", sampleVars(template));
        return ResponseEntity.ok(resp);
    }

    // ── Health / config check ────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("mailEnabled", props.isEnabled());
        resp.put("from", props.getFrom());
        resp.put("fromName", props.getFromName());
        resp.put("recipientMode", props.getRecipientMode().name());
        resp.put("defaultRecipient", props.getDefaultRecipient());
        resp.put("baseUrl", props.getBaseUrl());
        resp.put("templateCount", EmailTemplate.values().length);

        String status;
        if (!props.isEnabled()) {
            status = "DISABLED — emails are rendered + logged but never sent";
        } else if (props.getRecipientMode() == EmailProperties.RecipientMode.REDIRECT) {
            status = "REDIRECT — all emails go to " + props.getDefaultRecipient();
        } else if (props.getRecipientMode() == EmailProperties.RecipientMode.CC) {
            status = "CC — emails go to real recipient + copy to " + props.getDefaultRecipient();
        } else {
            status = "PRODUCTION — emails go to real recipients";
        }
        resp.put("status", status);
        return ResponseEntity.ok(resp);
    }

    // ── Send one test email ──────────────────────────────────────────

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> sendTest(
            @RequestParam(value = "to", required = false) String to,
            @RequestParam("template") EmailTemplate template,
            @RequestBody(required = false) Map<String, Object> customVars,
            Authentication authentication) {

        ResponseEntity<Map<String, Object>> limited = checkRateLimit(bucketKey(authentication, "test"), TEST_SEND_LIMIT_PER_MINUTE);
        if (limited != null) return limited;

        String recipient = resolveTestRecipient(to);
        if (recipient == null || recipient.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "No recipient — pass ?to= or set app.mail.default-recipient"));
        }
        if (!EMAIL_PATTERN.matcher(recipient).matches()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "\"" + recipient + "\" doesn't look like a valid email address"));
        }

        Map<String, Object> vars = sampleVars(template);
        if (customVars != null && !customVars.isEmpty()) {
            vars.putAll(customVars);
        }

        String html = renderer.render(template, vars);
        String subjectOverride = customVars != null ? asTrimmedString(customVars.get("subject")) : null;
        String subject = "[TEST] " + (subjectOverride != null && !subjectOverride.isBlank()
                ? subjectOverride : template.defaultSubject());

        String fromOverride = null;
        String fromNameOverride = null;
        if (customVars != null) {
            if (customVars.containsKey("fromEmail")) {
                fromOverride = String.valueOf(customVars.get("fromEmail"));
            }
            if (customVars.containsKey("fromName")) {
                fromNameOverride = String.valueOf(customVars.get("fromName"));
            }
        }

        emailService.send(new EmailMessage(recipient, "Test Recipient", subject, html, fromOverride, fromNameOverride));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("template", template.name());
        resp.put("subject", subject);
        resp.put("to", recipient);
        resp.put("customVarsApplied", customVars != null ? customVars.keySet() : List.of());
        resp.put("mailEnabled", props.isEnabled());
        resp.put("recipientMode", props.getRecipientMode().name());
        resp.put("note", props.isEnabled()
                ? "Dispatched via SMTP."
                : "app.mail.enabled=false — email was rendered + logged but not actually sent.");
        return ResponseEntity.ok(resp);
    }

    // ── Send ALL templates to one address ────────────────────────────

    @PostMapping("/test-all")
    public ResponseEntity<Map<String, Object>> sendAllTemplates(
            @RequestParam(value = "to", required = false) String to,
            @RequestBody(required = false) Map<String, Object> customVars,
            Authentication authentication) {

        ResponseEntity<Map<String, Object>> limited = checkRateLimit(bucketKey(authentication, "test-all"), TEST_SEND_ALL_LIMIT_PER_MINUTE);
        if (limited != null) return limited;

        String recipient = resolveTestRecipient(to);
        if (recipient == null || recipient.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "No recipient — pass ?to= or set app.mail.default-recipient"));
        }
        if (!EMAIL_PATTERN.matcher(recipient).matches()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "\"" + recipient + "\" doesn't look like a valid email address"));
        }

        List<Map<String, Object>> results = new ArrayList<>();
        int sent = 0;
        int failed = 0;

        String subjectOverride = customVars != null ? asTrimmedString(customVars.get("subject")) : null;

        for (EmailTemplate template : EmailTemplate.values()) {
            String subject = "[TEST] " + (subjectOverride != null && !subjectOverride.isBlank()
                    ? subjectOverride : template.defaultSubject());
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("template", template.name());
            entry.put("subject", subject);
            try {
                Map<String, Object> vars = sampleVars(template);
                if (customVars != null && !customVars.isEmpty()) {
                    vars.putAll(customVars);
                }
                String html = renderer.render(template, vars);

                String fromOverride = null;
                String fromNameOverride = null;
                if (customVars != null) {
                    if (customVars.containsKey("fromEmail")) {
                        fromOverride = String.valueOf(customVars.get("fromEmail"));
                    }
                    if (customVars.containsKey("fromName")) {
                        fromNameOverride = String.valueOf(customVars.get("fromName"));
                    }
                }

                emailService.send(new EmailMessage(recipient, "Test Recipient",
                        subject, html, fromOverride, fromNameOverride));
                entry.put("status", "SENT");
                sent++;
            } catch (Exception e) {
                entry.put("status", "FAILED");
                entry.put("error", e.getMessage());
                failed++;
                log.error("Failed to send test email for template {}: {}", template.name(), e.getMessage());
            }
            results.add(entry);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("to", recipient);
        resp.put("totalTemplates", EmailTemplate.values().length);
        resp.put("sent", sent);
        resp.put("failed", failed);
        resp.put("mailEnabled", props.isEnabled());
        resp.put("recipientMode", props.getRecipientMode().name());
        resp.put("results", results);
        resp.put("note", props.isEnabled()
                ? sent + " emails dispatched via SMTP to " + recipient
                : "app.mail.enabled=false — emails were rendered + logged but not actually sent.");
        return ResponseEntity.ok(resp);
    }

    // ── Resolve recipient: explicit > default-recipient > null ────────

    private String resolveTestRecipient(String explicit) {
        if (explicit != null && !explicit.isBlank()) return explicit.trim();
        if (props.getDefaultRecipient() != null && !props.getDefaultRecipient().isBlank()) {
            return props.getDefaultRecipient().trim();
        }
        return null;
    }

    // ── Per-admin rate limiting for real-email-dispatching endpoints ──

    private String bucketKey(Authentication authentication, String action) {
        String principal = authentication != null ? authentication.getName() : "anonymous";
        return action + ":" + principal;
    }

    /** Returns null when the call is allowed, or a ready-to-return 429 response when the limit is hit. */
    private ResponseEntity<Map<String, Object>> checkRateLimit(String key, int limitPerMinute) {
        long now = System.currentTimeMillis();
        RateLimitWindow window = rateLimitWindows.compute(key, (k, current) -> {
            if (current == null || now - current.start >= RATE_LIMIT_WINDOW_MS) {
                return new RateLimitWindow(now);
            }
            current.count.incrementAndGet();
            return current;
        });

        if (window.count.get() > limitPerMinute) {
            long retryAfter = Math.max(1, (RATE_LIMIT_WINDOW_MS - (now - window.start)) / 1000);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(retryAfter))
                    .body(Map.of("error", "Too many test emails sent — retry in " + retryAfter + "s"));
        }
        return null;
    }

    private static final class RateLimitWindow {
        final long start;
        final AtomicInteger count;

        RateLimitWindow(long start) {
            this.start = start;
            this.count = new AtomicInteger(1);
        }
    }

    // ── Small helper ───────────────────────────────────────────────────

    private static String asTrimmedString(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    // ── Sample data per template ─────────────────────────────────────

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
            case TOURNAMENT_OPEN -> {
                v.put("tournamentName", "Summer Smash Cup 2026");
                v.put("eventDate", "Sat, 20 Jun 2026");
                v.put("registrationDeadline", "Fri, 15 Jun 2026");
                v.put("contactName", "Vikram Patel");
                v.put("contactNumber", "+91 98765 43210");
                v.put("venueName", "Community Sports Arena");
                v.put("bannerImage", "");
                v.put("customMessage", "Don't miss your chance to compete! Early bird registrations get a free team jersey.");
                v.put("sportsEvents", List.of(
                        Map.of("sportName", "Badminton", "eventName", "Men's Singles", "icon", "🏸", "ageRange", "18–45", "gender", "Male"),
                        Map.of("sportName", "Badminton", "eventName", "Women's Doubles", "icon", "🏸", "ageRange", "18–40", "gender", "Female"),
                        Map.of("sportName", "Table Tennis", "eventName", "Mixed Doubles", "icon", "🏓", "ageRange", "16–50", "gender", "All"),
                        Map.of("sportName", "Cricket", "eventName", "Men's T20", "icon", "🏏", "ageRange", "18–45", "gender", "Male")
                ));
            }
            case TOURNAMENT_ANNOUNCEMENT -> {
                v.put("tournamentName", "Summer Smash Cup 2026");
                v.put("subject", "Important Update: Venue Change");
                v.put("customMessage", "Due to ongoing renovations, all matches have been moved to the Community Sports Arena (Block C). Same timings apply. See you there!");
                v.put("eventDate", "Sat, 20 Jun 2026");
                v.put("contactName", "Vikram Patel");
                v.put("contactNumber", "+91 98765 43210");
                v.put("bannerImage", "");
            }
            case EMAIL_OTP -> {
                v.put("otpCode", "428913");
                v.put("expiryMinutes", 10);
            }
            case REGISTRATION_OPEN -> {
                v.put("tournamentName", "Summer Smash Cup");
                v.put("description", "Join us for the annual Summer Smash Cup! Compete against local teams in multiple sports formats including Badminton, Table Tennis, and Football.");
                v.put("sportName", "Badminton, Table Tennis, Football");
                v.put("registrationPeriod", "15 Jun 2026 - 30 Jun 2026");
                v.put("eventDates", "05 Jul 2026 - 12 Jul 2026");
                v.put("venueName", "Community Sports Arena");
                v.put("contactDetails", "Amit Patel (amit@manacommunity.app, +91 98765 43210)");
                v.put("actionUrl", props.getBaseUrl() + "/sports");
            }
            case TOURNAMENT_START -> {
                v.put("tournamentName", "Summer Smash Cup 2026");
                v.put("description", "Welcome players and captains! The opening ceremony starts at 9:00 AM sharp at the central club lawns. First round matches will commence immediately after.");
                v.put("sportName", "Badminton, Football, Table Tennis");
                v.put("openingTime", "Sat, 05 Jul 2026 09:00 AM");
                v.put("venueName", "Community Sports Arena");
                v.put("firstFixture", "Team Red vs Team Blue (Football Pitch 1)");
                v.put("actionUrl", props.getBaseUrl() + "/sports");
            }
        }
        return v;
    }
}
