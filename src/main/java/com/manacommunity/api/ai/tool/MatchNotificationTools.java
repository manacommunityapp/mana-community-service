package com.manacommunity.api.ai.tool;

import com.manacommunity.api.model.Community;

import com.manacommunity.api.ai.config.AgentSecurityContext;
import com.manacommunity.api.ai.config.AgentSecurityContext.UserContext;
import com.manacommunity.api.email.EmailMessage;
import com.manacommunity.api.email.EmailService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.service.NotificationManagementService;
import com.manacommunity.api.model.NotificationType;
import com.manacommunity.api.model.NotificationCategory;
import com.manacommunity.api.model.NotificationPriority;
import com.manacommunity.api.model.ReferenceType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI tools for sending match schedules and creating reminders.
 *
 * <p>Bridges the AI agent to the existing email pipeline, in-app notification system,
 * and generates ICS calendar data for mobile calendar import.</p>
 *
 * <p>These tools go beyond read-only queries — they trigger real emails and
 * notifications. All are gated by confirmation prompts.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchNotificationTools {

    @PersistenceContext
    private EntityManager em;

    private final EmailService emailService;
    private final AppUserRepository userRepository;
    private final NotificationManagementService notificationService;

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final DateTimeFormatter ICS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    // ── MY MATCHES ─────────────────────────────────────────────────────

    @Tool(description = "Get the current user's personal match schedule — all their upcoming "
            + "and recent matches across all tournaments in the community, with team, venue, "
            + "court, and time details. This is the 'my matches' view.")
    public Object getMyMatches(
            @ToolParam(required = false, description = "Include completed matches too? (default false)")
            Boolean includeCompleted,
            @ToolParam(required = false, description = "Max results (default 15)") Integer limit) {

        UserContext ctx = AgentSecurityContext.get();

        String statusFilter = Boolean.TRUE.equals(includeCompleted)
                ? "m.status IN ('SCHEDULED', 'PUBLISHED', 'LIVE', 'COMPLETED')"
                : "m.status IN ('SCHEDULED', 'PUBLISHED', 'LIVE')";

        var results = em.createQuery(
                        "SELECT m.id, m.round, m.matchNumber, m.scheduledAt, m.durationMinutes, " +
                        "m.status, ta.teamName, tb.teamName, " +
                        "m.scoreTeamA, m.scoreTeamB, " +
                        "v.name, v.address, c.name, " +
                        "tc.tournamentName, s.name " +
                        "FROM TournamentMatch m " +
                        "LEFT JOIN m.teamA ta LEFT JOIN m.teamB tb " +
                        "LEFT JOIN m.venue v LEFT JOIN m.court c " +
                        "JOIN m.config tc LEFT JOIN tc.sport s " +
                        "JOIN tc.community com " +
                        "WHERE com.id = :comId AND " + statusFilter + " " +
                        "AND (ta.ownerUser.id = :uid OR tb.ownerUser.id = :uid " +
                        "OR ta.captainUser.id = :uid OR tb.captainUser.id = :uid " +
                        "OR ta.id IN (SELECT ap.assignedTeam.id FROM AuctionPlayer ap WHERE ap.user.id = :uid) " +
                        "OR tb.id IN (SELECT ap2.assignedTeam.id FROM AuctionPlayer ap2 WHERE ap2.user.id = :uid)) " +
                        "ORDER BY m.scheduledAt ASC", Object[].class)
                .setParameter("comId", ctx.communityId())
                .setParameter("uid", ctx.userId())
                .setMaxResults(limit != null ? limit : 15)
                .getResultList();

        return results.stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("match_id", r[0]);
                    m.put("round", r[1] != null ? r[1].toString() : null);
                    m.put("match_number", r[2]);
                    m.put("scheduled_at", r[3] != null ? ((LocalDateTime) r[3]).format(DISPLAY_FMT) : null);
                    m.put("scheduled_at_raw", r[3] != null ? r[3].toString() : null);
                    m.put("duration_minutes", r[4]);
                    m.put("status", r[5] != null ? r[5].toString() : null);
                    m.put("team_a", r[6]);
                    m.put("team_b", r[7]);
                    m.put("score_a", r[8]);
                    m.put("score_b", r[9]);
                    m.put("venue", r[10]);
                    m.put("venue_address", r[11]);
                    m.put("court", r[12]);
                    m.put("tournament", r[13]);
                    m.put("sport", r[14]);
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ── SEND SCHEDULE TO EMAIL ─────────────────────────────────────────

    @Tool(description = "Send the user's personal match schedule to their registered email. "
            + "Compiles all upcoming matches into a formatted email with venue, court, "
            + "time, and opponent details. Requires confirmation.")
    @Transactional
    public Object sendMyScheduleToEmail(
            @ToolParam(description = "Has the user explicitly confirmed they want the email sent?")
            Boolean confirmed) {

        UserContext ctx = AgentSecurityContext.get();

        if (!Boolean.TRUE.equals(confirmed)) {
            // Look up user email for the confirmation prompt
            String email = getUserEmail(ctx.userId());
            return Map.of("requires_confirmation", true,
                    "email", email != null ? email : "unknown",
                    "message", "Send your match schedule to " + email + "?");
        }

        AppUser user = userRepository.findById(ctx.userId()).orElse(null);
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return Map.of("error", true, "message", "No email address found for your account.");
        }

        // Fetch upcoming matches
        var matches = getUpcomingMatchData(ctx);

        if (matches.isEmpty()) {
            return Map.of("success", false, "message", "No upcoming matches found to send.");
        }

        // Build HTML email body
        StringBuilder html = new StringBuilder();
        html.append("<h2>Your Upcoming Match Schedule</h2>");
        html.append("<p>Hi ").append(user.getFullName()).append(",</p>");
        html.append("<p>Here are your upcoming matches:</p>");
        html.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse:collapse;'>");
        html.append("<tr style='background:#2563eb;color:white;'>");
        html.append("<th>Match</th><th>Teams</th><th>Date & Time</th><th>Venue</th><th>Court</th><th>Status</th>");
        html.append("</tr>");

        for (MatchData md : matches) {
            html.append("<tr>");
            html.append("<td>").append(md.tournament).append(" — ").append(md.round).append("</td>");
            html.append("<td><strong>").append(md.teamA).append("</strong> vs <strong>").append(md.teamB).append("</strong></td>");
            html.append("<td>").append(md.scheduledAt != null ? md.scheduledAt.format(DISPLAY_FMT) : "TBA").append("</td>");
            html.append("<td>").append(md.venue).append("</td>");
            html.append("<td>").append(md.court).append("</td>");
            html.append("<td>").append(md.status).append("</td>");
            html.append("</tr>");
        }
        html.append("</table>");
        html.append("<br><p>Good luck! 🏆</p>");
        html.append("<p style='color:#666;font-size:12px;'>— Mana Community AI Assistant</p>");

        emailService.send(new EmailMessage(
                user.getEmail(), user.getFullName(),
                "Your Match Schedule — " + matches.size() + " upcoming matches",
                html.toString()));

        log.info("Match schedule email sent to user={} ({}) with {} matches",
                ctx.userId(), user.getEmail(), matches.size());

        return Map.of("success", true,
                "email", user.getEmail(),
                "matches_included", matches.size(),
                "message", "Schedule sent to " + user.getEmail());
    }

    // ── CALENDAR DATA (ICS FORMAT) ─────────────────────────────────────

    @Tool(description = "Generate calendar event data for the user's matches in ICS format. "
            + "The user can use this to add matches to their phone calendar (Google Calendar, "
            + "Apple Calendar, Outlook). Returns one calendar event per match with title, "
            + "start/end time, location, and description.")
    public Object generateCalendarEvents(
            @ToolParam(required = false, description = "Specific match ID (omit for all upcoming matches)")
            Long matchId,
            @ToolParam(required = false, description = "Tournament config ID filter (omit for all)")
            Long tournamentConfigId) {

        UserContext ctx = AgentSecurityContext.get();

        List<MatchData> matches;
        if (matchId != null) {
            matches = getSingleMatchData(ctx, matchId);
        } else {
            matches = getUpcomingMatchData(ctx);
            if (tournamentConfigId != null) {
                matches = matches.stream()
                        .filter(m -> tournamentConfigId.equals(m.configId))
                        .collect(Collectors.toList());
            }
        }

        if (matches.isEmpty()) {
            return Map.of("info", "No upcoming matches found.",
                    "suggestion", "Try getMyMatches to see if you have any matches at all.");
        }

        List<Map<String, Object>> calendarEvents = new ArrayList<>();
        for (MatchData md : matches) {
            Map<String, Object> event = new LinkedHashMap<>();
            String title = md.teamA + " vs " + md.teamB + " — " + md.round;
            String location = md.venue + (md.court.isEmpty() ? "" : ", " + md.court);
            String description = "Tournament: " + md.tournament + "\n"
                    + "Sport: " + md.sport + "\n"
                    + "Round: " + md.round + "\n"
                    + "Match #" + md.matchNumber;

            event.put("title", title);
            event.put("location", location);
            event.put("description", description);
            event.put("match_id", md.matchId);

            if (md.scheduledAt != null) {
                event.put("start_time", md.scheduledAt.toString());
                event.put("start_time_display", md.scheduledAt.format(DISPLAY_FMT));

                LocalDateTime endTime = md.durationMinutes != null
                        ? md.scheduledAt.plusMinutes(md.durationMinutes)
                        : md.scheduledAt.plusHours(1);
                event.put("end_time", endTime.toString());

                // ICS format for calendar apps
                event.put("ics_dtstart", md.scheduledAt.format(ICS_FMT));
                event.put("ics_dtend", endTime.format(ICS_FMT));
            }

            event.put("calendar_add_instructions",
                    "To add to your phone calendar: "
                    + "open your Calendar app → Create new event → "
                    + "Title: \"" + title + "\" → "
                    + "Date/Time: " + (md.scheduledAt != null ? md.scheduledAt.format(DISPLAY_FMT) : "TBA") + " → "
                    + "Location: \"" + location + "\"");

            calendarEvents.add(event);
        }

        return Map.of(
                "calendar_events", calendarEvents,
                "total_events", calendarEvents.size(),
                "tip", "You can add these to your phone calendar manually, "
                        + "or ask me to send the schedule to your email.");
    }

    // ── IN-APP MATCH REMINDER ──────────────────────────────────────────

    @Tool(description = "Create an in-app notification reminder for a specific match. "
            + "The reminder appears in the user's notification inbox. Requires confirmation.")
    @Transactional
    public Object createMatchReminder(
            @ToolParam(description = "Match ID to create reminder for") Long matchId,
            @ToolParam(required = false, description = "Reminder message (optional, auto-generated if omitted)")
            String customMessage,
            @ToolParam(description = "Has the user explicitly confirmed?") Boolean confirmed) {

        UserContext ctx = AgentSecurityContext.get();

        // Fetch match details
        var rows = em.createQuery(
                        "SELECT m.id, m.round, m.scheduledAt, " +
                        "ta.teamName, tb.teamName, v.name, c.name, tc.tournamentName " +
                        "FROM TournamentMatch m " +
                        "LEFT JOIN m.teamA ta LEFT JOIN m.teamB tb " +
                        "LEFT JOIN m.venue v LEFT JOIN m.court c " +
                        "JOIN m.config tc " +
                        "WHERE m.id = :mid AND tc.community.id = :comId", Object[].class)
                .setParameter("mid", matchId)
                .setParameter("comId", ctx.communityId())
                .getResultList();

        if (rows.isEmpty()) return Map.of("error", "Match not found in your community");

        Object[] r = rows.get(0);
        String round = r[1] != null ? r[1].toString() : "Match";
        LocalDateTime scheduledAt = (LocalDateTime) r[2];
        String teamA = r[3] != null ? (String) r[3] : "TBD";
        String teamB = r[4] != null ? (String) r[4] : "TBD";
        String venue = r[5] != null ? (String) r[5] : "TBA";
        String tournament = (String) r[7];

        if (!Boolean.TRUE.equals(confirmed)) {
            return Map.of("requires_confirmation", true,
                    "match", teamA + " vs " + teamB,
                    "scheduled", scheduledAt != null ? scheduledAt.format(DISPLAY_FMT) : "TBA",
                    "message", "Create a reminder for " + teamA + " vs " + teamB + "?");
        }

        String title = "Match Reminder: " + teamA + " vs " + teamB;
        String body = customMessage != null ? customMessage
                : round + " — " + tournament + "\n"
                + (scheduledAt != null ? scheduledAt.format(DISPLAY_FMT) : "TBA")
                + " at " + venue;

        notificationService.createNotification(
                ctx.userId(),
                NotificationType.MATCH_REMINDER,
                NotificationCategory.SPORTS,
                title, body,
                "/profile?tab=schedule",
                ReferenceType.TOURNAMENT_MATCH, matchId,
                NotificationPriority.HIGH,
                null, ctx.communityId());

        log.info("Match reminder created for user={}, match={}", ctx.userId(), matchId);
        return Map.of("success", true, "match", teamA + " vs " + teamB,
                "message", "Reminder created! It will appear in your notifications.");
    }

    // ── SEND SPECIFIC MATCH DETAILS TO EMAIL ───────────────────────────

    @Tool(description = "Send details of a specific match to the user's email — includes "
            + "teams, venue, court, time, and calendar add instructions. Requires confirmation.")
    @Transactional
    public Object sendMatchDetailsToEmail(
            @ToolParam(description = "Match ID") Long matchId,
            @ToolParam(description = "Has the user explicitly confirmed?") Boolean confirmed) {

        UserContext ctx = AgentSecurityContext.get();

        List<MatchData> matches = getSingleMatchData(ctx, matchId);
        if (matches.isEmpty()) return Map.of("error", "Match not found in your community");

        MatchData md = matches.get(0);
        AppUser user = userRepository.findById(ctx.userId()).orElse(null);
        if (user == null || user.getEmail() == null) {
            return Map.of("error", true, "message", "No email address found.");
        }

        if (!Boolean.TRUE.equals(confirmed)) {
            return Map.of("requires_confirmation", true,
                    "match", md.teamA + " vs " + md.teamB,
                    "email", user.getEmail(),
                    "message", "Send match details to " + user.getEmail() + "?");
        }

        StringBuilder html = new StringBuilder();
        html.append("<h2>Match Details</h2>");
        html.append("<p>Hi ").append(user.getFullName()).append(",</p>");
        html.append("<table cellpadding='6' style='font-size:14px;'>");
        html.append("<tr><td><strong>Tournament:</strong></td><td>").append(md.tournament).append("</td></tr>");
        html.append("<tr><td><strong>Round:</strong></td><td>").append(md.round).append("</td></tr>");
        html.append("<tr><td><strong>Match:</strong></td><td>").append(md.teamA).append(" vs ").append(md.teamB).append("</td></tr>");
        html.append("<tr><td><strong>Date & Time:</strong></td><td>").append(
                md.scheduledAt != null ? md.scheduledAt.format(DISPLAY_FMT) : "TBA").append("</td></tr>");
        html.append("<tr><td><strong>Venue:</strong></td><td>").append(md.venue).append("</td></tr>");
        html.append("<tr><td><strong>Court:</strong></td><td>").append(md.court).append("</td></tr>");
        html.append("</table>");
        html.append("<br><p><strong>📅 Add to your calendar:</strong> Open your Calendar app and create an event with the details above.</p>");
        html.append("<p style='color:#666;font-size:12px;'>— Mana Community AI Assistant</p>");

        emailService.send(new EmailMessage(
                user.getEmail(), user.getFullName(),
                md.teamA + " vs " + md.teamB + " — Match Details",
                html.toString()));

        log.info("Match details email sent for match={} to user={}", matchId, ctx.userId());
        return Map.of("success", true, "email", user.getEmail());
    }

    // ── INTERNAL HELPERS ───────────────────────────────────────────────

    private record MatchData(
            Long matchId, Long configId, String round, Integer matchNumber,
            LocalDateTime scheduledAt, Integer durationMinutes, String status,
            String teamA, String teamB, String venue, String venueAddress,
            String court, String tournament, String sport) {}

    private List<MatchData> getUpcomingMatchData(UserContext ctx) {
        return em.createQuery(
                        "SELECT m.id, tc.id, m.round, m.matchNumber, m.scheduledAt, m.durationMinutes, " +
                        "m.status, ta.teamName, tb.teamName, " +
                        "v.name, v.address, c.name, tc.tournamentName, s.name " +
                        "FROM TournamentMatch m " +
                        "LEFT JOIN m.teamA ta LEFT JOIN m.teamB tb " +
                        "LEFT JOIN m.venue v LEFT JOIN m.court c " +
                        "JOIN m.config tc LEFT JOIN tc.sport s " +
                        "WHERE tc.community.id = :comId " +
                        "AND m.status IN ('SCHEDULED', 'PUBLISHED') " +
                        "AND (ta.ownerUser.id = :uid OR tb.ownerUser.id = :uid " +
                        "OR ta.captainUser.id = :uid OR tb.captainUser.id = :uid " +
                        "OR ta.id IN (SELECT ap.assignedTeam.id FROM AuctionPlayer ap WHERE ap.user.id = :uid) " +
                        "OR tb.id IN (SELECT ap2.assignedTeam.id FROM AuctionPlayer ap2 WHERE ap2.user.id = :uid)) " +
                        "ORDER BY m.scheduledAt ASC", Object[].class)
                .setParameter("comId", ctx.communityId())
                .setParameter("uid", ctx.userId())
                .setMaxResults(20)
                .getResultList().stream()
                .map(this::toMatchData)
                .collect(Collectors.toList());
    }

    private List<MatchData> getSingleMatchData(UserContext ctx, Long matchId) {
        return em.createQuery(
                        "SELECT m.id, tc.id, m.round, m.matchNumber, m.scheduledAt, m.durationMinutes, " +
                        "m.status, ta.teamName, tb.teamName, " +
                        "v.name, v.address, c.name, tc.tournamentName, s.name " +
                        "FROM TournamentMatch m " +
                        "LEFT JOIN m.teamA ta LEFT JOIN m.teamB tb " +
                        "LEFT JOIN m.venue v LEFT JOIN m.court c " +
                        "JOIN m.config tc LEFT JOIN tc.sport s " +
                        "WHERE m.id = :mid AND tc.community.id = :comId", Object[].class)
                .setParameter("mid", matchId)
                .setParameter("comId", ctx.communityId())
                .getResultList().stream()
                .map(this::toMatchData)
                .collect(Collectors.toList());
    }

    private MatchData toMatchData(Object[] r) {
        return new MatchData(
                (Long) r[0], (Long) r[1],
                r[2] != null ? r[2].toString() : "Match",
                (Integer) r[3],
                (LocalDateTime) r[4], (Integer) r[5],
                r[6] != null ? r[6].toString() : "UNKNOWN",
                r[7] != null ? (String) r[7] : "TBD",
                r[8] != null ? (String) r[8] : "TBD",
                r[9] != null ? (String) r[9] : "TBA",
                r[10] != null ? (String) r[10] : "",
                r[11] != null ? (String) r[11] : "",
                r[12] != null ? (String) r[12] : "Tournament",
                r[13] != null ? (String) r[13] : "");
    }

    private String getUserEmail(Long userId) {
        var rows = em.createQuery(
                        "SELECT u.email FROM AppUser u WHERE u.id = :uid", String.class)
                .setParameter("uid", userId)
                .getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }
}
