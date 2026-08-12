package com.manacommunity.api.controller;

import com.manacommunity.api.dto.email.EmailTemplateManagementDtos.PreviewResponse;
import com.manacommunity.api.dto.email.EmailTemplateManagementDtos.TemplateSummaryResponse;
import com.manacommunity.api.email.EmailMessage;
import com.manacommunity.api.email.EmailProperties;
import com.manacommunity.api.email.EmailService;
import com.manacommunity.api.email.EmailTemplate;
import com.manacommunity.api.email.EmailTemplateManagementService;
import com.manacommunity.api.email.EmailTemplateRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/admin/email")
@RequiredArgsConstructor
public class MainEmailVerificationController {

    private final EmailTemplateManagementService templateService;
    private final EmailService emailService;
    private final EmailProperties props;
    private final EmailTemplateRenderer emailTemplateRenderer;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private static final int TEST_SEND_LIMIT_PER_MINUTE = 10;
    private static final int TEST_SEND_ALL_LIMIT_PER_MINUTE = 2;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000L;
    private final ConcurrentHashMap<String, RateLimitWindow> rateLimitWindows = new ConcurrentHashMap<>();

    @GetMapping("/templates")
    public ResponseEntity<Map<String, Object>> templates(
            @RequestParam(value = "communityId", required = false) Long communityId
    ) {
        List<TemplateSummaryResponse> templates = communityId == null
                ? templateService.listAllTemplates()
                : templateService.listTemplates(communityId);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("source", "DATABASE");
        resp.put("communityId", communityId);
        resp.put("count", templates.size());
        resp.put("templates", templates);
        return ResponseEntity.ok(resp);
    }

    /**
     * The built-in (code-shipped) version of a template — as opposed to
     * {@link #templates} / {@link #preview}, which serve the DB-stored, per-community
     * customizable version. Backs the "Default HTML" view in the admin template UI.
     */
    @GetMapping("/default-template/{templateCode}")
    public ResponseEntity<Map<String, Object>> defaultTemplate(@PathVariable String templateCode) {
        EmailTemplate template;
        try {
            template = EmailTemplate.valueOf(templateCode.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }

        String fileStem = template.templateName().substring("email/".length());
        String templateFile = fileStem + ".html";
        String rawHtml;
        try {
            rawHtml = new ClassPathResource("templates/email/" + templateFile)
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read default template file {}: {}", templateFile, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }

        String renderedHtml = emailTemplateRenderer.render(template, sampleVars(template.name()));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("key", template.name());
        resp.put("templateName", humanize(template.name()));
        resp.put("templateFile", templateFile);
        resp.put("subject", template.defaultSubject());
        resp.put("category", template.category().name());
        resp.put("rawHtml", rawHtml);
        resp.put("renderedHtml", renderedHtml);
        return ResponseEntity.ok(resp);
    }

    @GetMapping(value = "/preview/{templateCode}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> preview(
            @PathVariable String templateCode,
            @RequestParam("communityId") Long communityId
    ) {
        PreviewResponse preview = templateService.previewActiveTemplate(
                communityId,
                templateCode,
                sampleVars(templateCode)
        );
        return ResponseEntity.ok(preview.html());
    }

    @PostMapping(value = "/preview/{templateCode}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> previewPost(
            @PathVariable String templateCode,
            @RequestParam("communityId") Long communityId,
            @RequestBody(required = false) Map<String, Object> customVars
    ) {
        Map<String, Object> vars = sampleVars(templateCode);
        if (customVars != null) {
            vars.putAll(customVars);
        }

        PreviewResponse preview = templateService.previewActiveTemplate(communityId, templateCode, vars);
        return ResponseEntity.ok(preview.html());
    }

    @GetMapping("/sample-payloads")
    public ResponseEntity<Map<String, Object>> allSamplePayloads(
            @RequestParam(value = "communityId", required = false) Long communityId
    ) {
        List<TemplateSummaryResponse> templates = communityId == null
                ? templateService.listAllTemplates()
                : templateService.listTemplates(communityId);

        Map<String, Object> payloads = new LinkedHashMap<>();
        for (TemplateSummaryResponse template : templates) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("communityId", template.communityId());
            entry.put("subject", template.subject());
            entry.put("variables", sampleVars(template.templateCode()));
            payloads.put(template.communityId() + ":" + template.templateCode(), entry);
        }
        return ResponseEntity.ok(payloads);
    }

    @GetMapping("/sample-payloads/{templateCode}")
    public ResponseEntity<Map<String, Object>> samplePayload(
            @PathVariable String templateCode,
            @RequestParam(value = "communityId", required = false) Long communityId
    ) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("templateCode", normalizeCode(templateCode));
        resp.put("communityId", communityId);
        resp.put("variables", sampleVars(templateCode));
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health(
            @RequestParam(value = "communityId", required = false) Long communityId
    ) {
        int templateCount = communityId == null
                ? templateService.listAllTemplates().size()
                : templateService.listTemplates(communityId).size();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("mailEnabled", props.isEnabled());
        resp.put("from", props.getFrom());
        resp.put("fromName", props.getFromName());
        resp.put("recipientMode", props.getRecipientMode().name());
        resp.put("defaultRecipient", props.getDefaultRecipient());
        resp.put("baseUrl", props.getBaseUrl());
        resp.put("templateSource", "DATABASE");
        resp.put("templateCount", templateCount);

        String status;
        if (!props.isEnabled()) {
            status = "DISABLED - emails are rendered and logged but never sent";
        } else if (props.getRecipientMode() == EmailProperties.RecipientMode.REDIRECT) {
            status = "REDIRECT - all emails go to " + props.getDefaultRecipient();
        } else if (props.getRecipientMode() == EmailProperties.RecipientMode.CC) {
            status = "CC - emails go to real recipient plus copy to " + props.getDefaultRecipient();
        } else {
            status = "PRODUCTION - emails go to real recipients";
        }
        resp.put("status", status);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> sendTest(
            @RequestParam(value = "to", required = false) String to,
            @RequestParam("communityId") Long communityId,
            @RequestParam("template") String templateCode,
            @RequestBody(required = false) Map<String, Object> customVars,
            Authentication authentication
    ) {
        ResponseEntity<Map<String, Object>> limited = checkRateLimit(bucketKey(authentication, "test"), TEST_SEND_LIMIT_PER_MINUTE);
        if (limited != null) return limited;

        String recipient = resolveTestRecipient(to);
        if (recipient == null || recipient.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "No recipient - pass ?to= or set app.mail.default-recipient"));
        }
        if (!EMAIL_PATTERN.matcher(recipient).matches()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "\"" + recipient + "\" doesn't look like a valid email address"));
        }

        Map<String, Object> vars = sampleVars(templateCode);
        if (customVars != null && !customVars.isEmpty()) {
            vars.putAll(customVars);
        }

        PreviewResponse preview = templateService.previewActiveTemplate(communityId, templateCode, vars);
        String subject = "[TEST] " + preview.subject();
        emailService.send(new EmailMessage(
                recipient,
                "Test Recipient",
                subject,
                preview.html(),
                stringValue(customVars, "fromEmail"),
                stringValue(customVars, "fromName")
        ));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("source", "DATABASE");
        resp.put("communityId", communityId);
        resp.put("template", normalizeCode(templateCode));
        resp.put("subject", subject);
        resp.put("to", recipient);
        resp.put("customVarsApplied", customVars != null ? customVars.keySet() : List.of());
        resp.put("mailEnabled", props.isEnabled());
        resp.put("recipientMode", props.getRecipientMode().name());
        resp.put("note", props.isEnabled()
                ? "Dispatched via SMTP."
                : "app.mail.enabled=false - email was rendered and logged but not actually sent.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/test-all")
    public ResponseEntity<Map<String, Object>> sendAllTemplates(
            @RequestParam(value = "to", required = false) String to,
            @RequestParam("communityId") Long communityId,
            @RequestBody(required = false) Map<String, Object> customVars,
            Authentication authentication
    ) {
        ResponseEntity<Map<String, Object>> limited = checkRateLimit(bucketKey(authentication, "test-all"), TEST_SEND_ALL_LIMIT_PER_MINUTE);
        if (limited != null) return limited;

        String recipient = resolveTestRecipient(to);
        if (recipient == null || recipient.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "No recipient - pass ?to= or set app.mail.default-recipient"));
        }
        if (!EMAIL_PATTERN.matcher(recipient).matches()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "\"" + recipient + "\" doesn't look like a valid email address"));
        }

        List<TemplateSummaryResponse> templates = templateService.listTemplates(communityId);
        List<Map<String, Object>> results = new ArrayList<>();
        int sent = 0;
        int failed = 0;

        for (TemplateSummaryResponse template : templates) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("template", template.templateCode());
            try {
                Map<String, Object> vars = sampleVars(template.templateCode());
                if (customVars != null && !customVars.isEmpty()) {
                    vars.putAll(customVars);
                }
                PreviewResponse preview = templateService.previewActiveTemplate(
                        communityId,
                        template.templateCode(),
                        vars
                );

                emailService.send(new EmailMessage(
                        recipient,
                        "Test Recipient",
                        "[TEST] " + preview.subject(),
                        preview.html(),
                        stringValue(customVars, "fromEmail"),
                        stringValue(customVars, "fromName")
                ));
                entry.put("subject", "[TEST] " + preview.subject());
                entry.put("status", "SENT");
                sent++;
            } catch (Exception e) {
                entry.put("status", "FAILED");
                entry.put("error", e.getMessage());
                failed++;
                log.error("Failed to send DB test email for template {}: {}", template.templateCode(), e.getMessage());
            }
            results.add(entry);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("source", "DATABASE");
        resp.put("communityId", communityId);
        resp.put("to", recipient);
        resp.put("totalTemplates", templates.size());
        resp.put("sent", sent);
        resp.put("failed", failed);
        resp.put("mailEnabled", props.isEnabled());
        resp.put("recipientMode", props.getRecipientMode().name());
        resp.put("results", results);
        resp.put("note", props.isEnabled()
                ? sent + " emails dispatched via SMTP to " + recipient
                : "app.mail.enabled=false - emails were rendered and logged but not actually sent.");
        return ResponseEntity.ok(resp);
    }

    private String resolveTestRecipient(String explicit) {
        if (explicit != null && !explicit.isBlank()) {
            return explicit.trim();
        }
        if (props.getDefaultRecipient() != null && !props.getDefaultRecipient().isBlank()) {
            return props.getDefaultRecipient().trim();
        }
        return null;
    }

    private String bucketKey(Authentication authentication, String action) {
        String principal = authentication != null ? authentication.getName() : "anonymous";
        return action + ":" + principal;
    }

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

    private Map<String, Object> sampleVars(String templateCode) {
        Map<String, Object> v = new HashMap<>();
        v.put("appName", props.getFromName());
        v.put("baseUrl", props.getBaseUrl());
        v.put("recipientName", "Rahul Sharma");
        v.put("firstName", "Rahul");
        v.put("lastName", "Sharma");
        v.put("year", LocalDate.now().getYear());
        v.put("actionUrl", props.getBaseUrl() + "/profile");

        switch (normalizeCode(templateCode)) {
            case "REGISTRATION_RECEIVED" -> {
                v.put("eventName", "Summer Smash Badminton 2026");
                v.put("sportName", "Badminton");
                v.put("categoryName", "Men's Open");
                v.put("matchType", "Singles");
                v.put("eventDates", "Sat, 20 Jun 2026");
                v.put("venueName", "Community Sports Arena");
                v.put("status", "Registered");
            }
            case "REGISTRATION_CONFIRMED" -> {
                v.put("eventName", "Summer Smash Badminton 2026");
                v.put("sportName", "Badminton");
                v.put("categoryName", "Men's Open");
                v.put("matchType", "Singles");
                v.put("eventDates", "Sat, 20 Jun 2026");
                v.put("venueName", "Community Sports Arena");
                v.put("confirmedAt", "Sat, 14 Jun 2026 10:30 AM");
            }
            case "REGISTRATION_REJECTED" -> {
                v.put("eventName", "Summer Smash Badminton 2026");
                v.put("sportName", "Badminton");
                v.put("categoryName", "Men's Open");
                v.put("eventDates", "Sat, 20 Jun 2026");
                v.put("venueName", "Community Sports Arena");
                v.put("reason", "The Men's Open category reached its maximum number of participants.");
            }
            case "SCHEDULE_PUBLISHED" -> {
                v.put("tournamentName", "Summer Smash Cup");
                v.put("eventName", "Summer Smash Badminton 2026");
                v.put("sportName", "Badminton");
                v.put("startDate", "Sat, 20 Jun 2026");
                v.put("venueName", "Community Sports Arena");
                v.put("matchCount", 24);
            }
            case "MATCH_REMINDER" -> {
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
            case "WINNER_NOTIFICATION" -> {
                v.put("tournamentName", "Summer Smash Cup");
                v.put("roundName", "Quarter Final");
                v.put("opponentName", "Amit Kumar");
                v.put("score", "21-18, 21-15");
                v.put("nextRoundInfo", "You play the winner of QF-2 in the Semi-Final on Sun, 21 Jun.");
            }
            case "TOURNAMENT_COMPLETION" -> {
                v.put("tournamentName", "Summer Smash Cup");
                v.put("championName", "Rahul Sharma");
                v.put("runnerUpName", "Amit Kumar");
                v.put("thirdPlaceName", "Vikram Singh");
            }
            case "PRIZE_DISTRIBUTION" -> {
                v.put("tournamentName", "Summer Smash Cup");
                v.put("position", "Champion");
                v.put("prize", "10000 + Trophy");
                v.put("ceremonyDate", "Sun, 21 Jun 2026, 06:00 PM");
                v.put("venueName", "Community Sports Arena");
            }
            case "TOURNAMENT_OPEN" -> {
                v.put("tournamentName", "Summer Smash Cup 2026");
                v.put("eventDate", "Sat, 20 Jun 2026");
                v.put("registrationDeadline", "Fri, 15 Jun 2026");
                v.put("contactName", "Vikram Patel");
                v.put("contactNumber", "+91 98765 43210");
                v.put("venueName", "Community Sports Arena");
                v.put("bannerImage", "");
                v.put("sportList", "Badminton, Table Tennis, Cricket");
                v.put("customMessage", "Early registrations get priority fixture slots.");
                v.put("actionUrl", props.getBaseUrl() + "/sports");
            }
            case "TOURNAMENT_ANNOUNCEMENT" -> {
                v.put("tournamentName", "Summer Smash Cup 2026");
                v.put("tournamentDescription", "A community-wide tournament across badminton, cricket, and table tennis.");
                v.put("customMessage", "Venue updated to Community Sports Arena, Block C. Timings remain unchanged.");
                v.put("eventStartDate", "20 Jun 2026");
                v.put("totalEvents", 5);
                v.put("totalSports", 3);
                v.put("expectedParticipants", 500);
                v.put("venueName", "Community Sports Arena");
                v.put("supportEmail", "sports@manacommunity.app");
                v.put("supportPhone", "+91 98765 43210");
                v.put("actionUrl", props.getBaseUrl() + "/sports");
                v.put("actionButtonText", "Register Now →");
                v.put("footerText", "Bringing Communities Together Through Sports 🏆");
                v.put("bannerImage", "");
                v.put("sportsEvents", List.of(
                        Map.of("icon", "🏸", "iconBgColor", "#2563eb", "eventName", "Badminton — Men's Singles",
                                "gender", "Open", "eventDate", "Sat, 20 Jun 2026", "venueName", "Community Sports Arena",
                                "ageRange", "18+ years"),
                        Map.of("icon", "🏏", "iconBgColor", "#16a34a", "eventName", "Cricket — Community League",
                                "gender", "Mixed", "eventDate", "Sun, 21 Jun 2026", "venueName", "Community Sports Arena",
                                "ageRange", "All ages")
                ));
                v.put("sportsIncluded", List.of(
                        Map.of("icon", "🏸", "sportName", "Badminton"),
                        Map.of("icon", "🏏", "sportName", "Cricket"),
                        Map.of("icon", "🏓", "sportName", "Table Tennis")
                ));
                v.put("galleryImages", List.of(
                        Map.of("icon", "🏆", "title", "Last Year's Finals", "bgColor", "#2563eb", "imageUrl", ""),
                        Map.of("icon", "🎉", "title", "Opening Ceremony", "bgColor", "#16a34a", "imageUrl", "")
                ));
                v.put("timeline", List.of(
                        Map.of("date", "15 Jun", "title", "Registrations Close", "description", "Last day to register your team."),
                        Map.of("date", "20 Jun", "title", "Tournament Begins", "description", "Opening matches start at 9:00 AM."),
                        Map.of("date", "28 Jun", "title", "Finals & Prize Ceremony", "description", "")
                ));
                v.put("announcements", List.of(
                        Map.of("icon", "📢", "title", "Venue Update", "content", "Block C courts are now open for warm-ups from 8:00 AM."),
                        Map.of("icon", "🎽", "title", "", "content", "Team jerseys can be collected from the sports office.")
                ));
            }
            case "EMAIL_OTP" -> {
                v.put("otpCode", "428913");
                v.put("expiryMinutes", 10);
            }
            case "REGISTRATION_OPEN" -> {
                v.put("tournamentName", "Summer Smash Cup");
                v.put("description", "Join us for the annual Summer Smash Cup across multiple sports.");
                v.put("sportName", "Badminton, Table Tennis, Football");
                v.put("sportList", "Badminton, Table Tennis, Football");
                v.put("registrationPeriod", "15 Jun 2026 - 30 Jun 2026");
                v.put("eventDates", "05 Jul 2026 - 12 Jul 2026");
                v.put("venueName", "Community Sports Arena");
                v.put("contactDetails", "Amit Patel, +91 98765 43210");
                v.put("actionUrl", props.getBaseUrl() + "/sports");
            }
            case "TOURNAMENT_START" -> {
                v.put("tournamentName", "Summer Smash Cup 2026");
                v.put("description", "Opening ceremony starts at 9:00 AM sharp.");
                v.put("sportName", "Badminton, Football, Table Tennis");
                v.put("openingTime", "Sat, 05 Jul 2026 09:00 AM");
                v.put("venueName", "Community Sports Arena");
                v.put("firstFixture", "Team Red vs Team Blue");
                v.put("actionUrl", props.getBaseUrl() + "/sports");
            }
            default -> {
                v.put("tournamentName", "Summer Smash Cup");
                v.put("eventName", "Community Event");
                v.put("venueName", "Community Sports Arena");
            }
        }
        return v;
    }

    private String normalizeCode(String templateCode) {
        return templateCode == null ? "" : templateCode.trim().toUpperCase(Locale.ROOT);
    }

    private String humanize(String enumConstantName) {
        StringBuilder sb = new StringBuilder();
        for (String part : enumConstantName.split("_")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase(Locale.ROOT))
                        .append(' ');
            }
        }
        return sb.toString().trim();
    }

    private String stringValue(Map<String, Object> vars, String key) {
        if (vars == null || !vars.containsKey(key) || vars.get(key) == null) {
            return null;
        }
        String value = String.valueOf(vars.get(key)).trim();
        return value.isEmpty() ? null : value;
    }
}
