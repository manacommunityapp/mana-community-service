package com.manacommunity.api.ai.tool;

import com.manacommunity.api.model.Community;

import com.manacommunity.api.ai.config.AgentSecurityContext;
import com.manacommunity.api.ai.config.AgentSecurityContext.UserContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates one-tap deep links for adding matches to phone calendars.
 *
 * <p>Google Calendar: {@code https://calendar.google.com/calendar/r/eventedit?...}</p>
 * <p>Apple Calendar / Outlook: {@code webcal://} or manual ICS data</p>
 *
 * <p>The user taps the link → their calendar app opens with the event pre-filled →
 * they tap Save. Zero typing needed.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
@Transactional(readOnly = true)
public class CalendarDeepLinkTools {

    @PersistenceContext
    private EntityManager em;

    private static final DateTimeFormatter ICS = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM, hh:mm a");

    @Tool(description = "Generate one-tap calendar links for a specific match. Returns a Google "
            + "Calendar link the user can tap to instantly add the match to their calendar. "
            + "Also provides Apple Calendar / Outlook data. Community-scoped, read-only.")
    public Object getCalendarLinks(
            @ToolParam(description = "Match ID") Long matchId) {

        UserContext ctx = AgentSecurityContext.get();

        var rows = em.createQuery(
                "SELECT m.scheduledAt, m.durationMinutes, m.round, " +
                "ta.teamName, tb.teamName, v.name, v.address, c.name, tc.tournamentName " +
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
        LocalDateTime start = (LocalDateTime) r[0];
        Integer duration = (Integer) r[1];
        String round = r[2] != null ? r[2].toString() : "Match";
        String teamA = r[3] != null ? (String) r[3] : "TBD";
        String teamB = r[4] != null ? (String) r[4] : "TBD";
        String venue = r[5] != null ? (String) r[5] : "";
        String address = r[6] != null ? (String) r[6] : "";
        String court = r[7] != null ? (String) r[7] : "";
        String tournament = (String) r[8];

        if (start == null) {
            return Map.of("error", "Match time not set yet — calendar links need a scheduled date/time.");
        }

        LocalDateTime end = start.plusMinutes(duration != null ? duration : 60);
        String title = teamA + " vs " + teamB + " — " + round;
        String location = venue + (!court.isEmpty() ? ", " + court : "")
                + (!address.isEmpty() ? ", " + address : "");
        String description = "Tournament: " + tournament + "\\nRound: " + round
                + "\\nVenue: " + location;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("match", teamA + " vs " + teamB);
        result.put("when", start.format(DISPLAY));
        result.put("where", location);

        // Google Calendar deep link
        String googleLink = "https://calendar.google.com/calendar/r/eventedit"
                + "?text=" + encode(title)
                + "&dates=" + start.format(ICS) + "/" + end.format(ICS)
                + "&location=" + encode(location)
                + "&details=" + encode(description.replace("\\n", "\n"));
        result.put("google_calendar_link", googleLink);
        result.put("google_calendar_instruction", "Tap this link to add to Google Calendar instantly");

        // Outlook.com deep link
        String outlookLink = "https://outlook.live.com/calendar/0/deeplink/compose"
                + "?subject=" + encode(title)
                + "&startdt=" + start.toString()
                + "&enddt=" + end.toString()
                + "&location=" + encode(location)
                + "&body=" + encode(description.replace("\\n", "\n"));
        result.put("outlook_link", outlookLink);

        // ICS raw data for Apple Calendar / manual import
        Map<String, String> icsData = new LinkedHashMap<>();
        icsData.put("DTSTART", start.format(ICS));
        icsData.put("DTEND", end.format(ICS));
        icsData.put("SUMMARY", title);
        icsData.put("LOCATION", location);
        icsData.put("DESCRIPTION", "Tournament: " + tournament + " | Round: " + round);
        icsData.put("REMINDER", "30 minutes before");
        result.put("ics_data", icsData);
        result.put("apple_calendar_instruction",
                "Open your Calendar app → tap + → paste: Title: \"" + title
                + "\", Date: " + start.format(DISPLAY) + ", Location: \"" + location + "\"");

        return result;
    }

    @Tool(description = "Generate calendar links for ALL upcoming matches of the current user. "
            + "Returns a Google Calendar link and ICS data for each match. "
            + "Community-scoped, read-only.")
    public Object getCalendarLinksForAllMyMatches() {

        UserContext ctx = AgentSecurityContext.get();

        var matches = em.createQuery(
                "SELECT m.id, m.scheduledAt, m.durationMinutes, m.round, " +
                "ta.teamName, tb.teamName, v.name, v.address, c.name, tc.tournamentName " +
                "FROM TournamentMatch m " +
                "LEFT JOIN m.teamA ta LEFT JOIN m.teamB tb " +
                "LEFT JOIN m.venue v LEFT JOIN m.court c " +
                "JOIN m.config tc " +
                "WHERE tc.community.id = :comId " +
                "AND m.status IN ('SCHEDULED', 'PUBLISHED') " +
                "AND m.scheduledAt IS NOT NULL " +
                "AND (ta.ownerUser.id = :uid OR tb.ownerUser.id = :uid " +
                "OR ta.captainUser.id = :uid OR tb.captainUser.id = :uid " +
                "OR ta.id IN (SELECT ap.assignedTeam.id FROM AuctionPlayer ap WHERE ap.user.id = :uid) " +
                "OR tb.id IN (SELECT ap2.assignedTeam.id FROM AuctionPlayer ap2 WHERE ap2.user.id = :uid)) " +
                "ORDER BY m.scheduledAt ASC", Object[].class)
                .setParameter("comId", ctx.communityId())
                .setParameter("uid", ctx.userId())
                .setMaxResults(10)
                .getResultList();

        if (matches.isEmpty()) {
            return Map.of("info", "No upcoming scheduled matches found.");
        }

        List<Map<String, Object>> events = new ArrayList<>();
        for (Object[] r : matches) {
            LocalDateTime start = (LocalDateTime) r[1];
            int duration = r[2] != null ? (Integer) r[2] : 60;
            LocalDateTime end = start.plusMinutes(duration);

            String teamA = r[4] != null ? (String) r[4] : "TBD";
            String teamB = r[5] != null ? (String) r[5] : "TBD";
            String round = r[3] != null ? r[3].toString() : "Match";
            String venue = r[6] != null ? (String) r[6] : "";
            String court = r[8] != null ? (String) r[8] : "";
            String tournament = (String) r[9];

            String title = teamA + " vs " + teamB + " — " + round;
            String location = venue + (!court.isEmpty() ? ", " + court : "");

            String googleLink = "https://calendar.google.com/calendar/r/eventedit"
                    + "?text=" + encode(title)
                    + "&dates=" + start.format(ICS) + "/" + end.format(ICS)
                    + "&location=" + encode(location)
                    + "&details=" + encode(tournament + " | " + round);

            Map<String, Object> event = new LinkedHashMap<>();
            event.put("match_id", r[0]);
            event.put("match", teamA + " vs " + teamB);
            event.put("round", round);
            event.put("when", start.format(DISPLAY));
            event.put("where", location);
            event.put("google_calendar_link", googleLink);
            events.add(event);
        }

        return Map.of("matches", events, "total", events.size(),
                "tip", "Share any Google Calendar link to add that match to your calendar in one tap.");
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
