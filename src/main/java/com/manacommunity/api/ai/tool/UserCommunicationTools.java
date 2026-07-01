package com.manacommunity.api.ai.tool;

import com.manacommunity.api.ai.config.AgentSecurityContext;
import com.manacommunity.api.ai.config.AgentSecurityContext.UserContext;
import com.manacommunity.api.ai.service.AiWebSocketPushService;
import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.repository.AppUserRepository;
import com.manacommunity.api.sms.SmsService;
import com.manacommunity.api.sms.SmsService.SmsResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI tools for sending match and schedule information via SMS, WhatsApp,
 * and real-time WebSocket push notifications.
 *
 * <p>Every send operation requires explicit user confirmation. Phone numbers
 * are resolved from the user's account — never taken from the chat input.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCommunicationTools {

    @PersistenceContext
    private EntityManager em;

    private final SmsService smsService;
    private final AppUserRepository userRepository;
    private final AiWebSocketPushService webSocketPush;

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd MMM, hh:mm a");

    // ── SMS ─────────────────────────────────────────────────────────────

    @Tool(description = "Send the user's upcoming match schedule to their registered mobile "
            + "number via SMS. Compiles matches into a concise text message. Requires confirmation. "
            + "The phone number comes from the user's account, not from the chat.")
    @Transactional
    public Object sendScheduleViaSms(
            @ToolParam(description = "Has the user explicitly confirmed?") Boolean confirmed) {

        UserContext ctx = AgentSecurityContext.get();
        AppUser user = userRepository.findById(ctx.userId()).orElse(null);

        if (user == null || user.getPhone() == null || user.getPhone().isBlank()) {
            return Map.of("error", true, "message",
                    "No phone number found on your account. Please update your profile.");
        }

        List<MatchInfo> matches = getUpcomingMatches(ctx);
        if (matches.isEmpty()) {
            return Map.of("info", "No upcoming matches found to send.");
        }

        String maskedPhone = maskPhone(user.getPhone());

        if (!Boolean.TRUE.equals(confirmed)) {
            return Map.of("requires_confirmation", true,
                    "phone", maskedPhone,
                    "matches", matches.size(),
                    "message", "Send " + matches.size() + " match details to " + maskedPhone + " via SMS?");
        }

        StringBuilder sms = new StringBuilder();
        sms.append("🏏 Your Matches:\n\n");
        for (int i = 0; i < Math.min(matches.size(), 5); i++) { // SMS limit: max 5 matches
            MatchInfo m = matches.get(i);
            sms.append(m.teamA).append(" vs ").append(m.teamB).append("\n");
            sms.append(m.scheduledAt != null ? m.scheduledAt.format(DISPLAY_FMT) : "TBA");
            sms.append(" @ ").append(m.venue).append("\n\n");
        }
        if (matches.size() > 5) {
            sms.append("+ ").append(matches.size() - 5).append(" more matches\n");
        }
        sms.append("— Mana Community");

        SmsResult result = smsService.sendSms(user.getPhone(), sms.toString());

        log.info("SMS schedule sent to user={}, phone={}, success={}", ctx.userId(), maskedPhone, result.success());
        return Map.of("success", result.success(),
                "phone", maskedPhone,
                "matches_sent", Math.min(matches.size(), 5),
                "message", result.success()
                        ? "Schedule sent to " + maskedPhone
                        : "Failed to send: " + result.error());
    }

    // ── WHATSAPP ────────────────────────────────────────────────────────

    @Tool(description = "Send the user's upcoming match schedule to their registered mobile "
            + "number via WhatsApp. Includes rich formatting with emojis. Requires confirmation. "
            + "The phone number comes from the user's account.")
    @Transactional
    public Object sendScheduleViaWhatsApp(
            @ToolParam(description = "Has the user explicitly confirmed?") Boolean confirmed) {

        UserContext ctx = AgentSecurityContext.get();
        AppUser user = userRepository.findById(ctx.userId()).orElse(null);

        if (user == null || user.getPhone() == null || user.getPhone().isBlank()) {
            return Map.of("error", true, "message", "No phone number on your account.");
        }

        List<MatchInfo> matches = getUpcomingMatches(ctx);
        if (matches.isEmpty()) {
            return Map.of("info", "No upcoming matches found.");
        }

        String maskedPhone = maskPhone(user.getPhone());

        if (!Boolean.TRUE.equals(confirmed)) {
            return Map.of("requires_confirmation", true,
                    "phone", maskedPhone,
                    "matches", matches.size(),
                    "message", "Send " + matches.size() + " match details to " + maskedPhone + " via WhatsApp?");
        }

        StringBuilder msg = new StringBuilder();
        msg.append("📅 *Your Match Schedule*\n");
        msg.append("━━━━━━━━━━━━━━━\n\n");
        for (MatchInfo m : matches) {
            msg.append("🏆 *").append(m.tournament).append("*\n");
            msg.append("⚔️ ").append(m.teamA).append(" vs ").append(m.teamB).append("\n");
            msg.append("🗓 ").append(m.scheduledAt != null ? m.scheduledAt.format(DISPLAY_FMT) : "TBA").append("\n");
            msg.append("📍 ").append(m.venue);
            if (!m.court.isEmpty()) msg.append(" — ").append(m.court);
            msg.append("\n");
            msg.append("🔵 ").append(m.round).append("\n\n");
        }
        msg.append("_Good luck!_ 🎯\n— Mana Community");

        SmsResult result = smsService.sendWhatsApp(user.getPhone(), msg.toString());

        log.info("WhatsApp schedule sent to user={}, success={}", ctx.userId(), result.success());
        return Map.of("success", result.success(),
                "phone", maskedPhone,
                "matches_sent", matches.size(),
                "message", result.success()
                        ? "Schedule sent via WhatsApp to " + maskedPhone
                        : "Failed: " + result.error());
    }

    @Tool(description = "Send details of a specific match to the user's phone via SMS or WhatsApp. "
            + "Requires confirmation.")
    @Transactional
    public Object sendMatchToPhone(
            @ToolParam(description = "Match ID") Long matchId,
            @ToolParam(description = "Channel: SMS or WHATSAPP") String channel,
            @ToolParam(description = "Has the user explicitly confirmed?") Boolean confirmed) {

        UserContext ctx = AgentSecurityContext.get();
        AppUser user = userRepository.findById(ctx.userId()).orElse(null);

        if (user == null || user.getPhone() == null || user.getPhone().isBlank()) {
            return Map.of("error", true, "message", "No phone number on your account.");
        }

        var rows = em.createQuery(
                        "SELECT m.round, m.scheduledAt, ta.teamName, tb.teamName, " +
                        "v.name, v.address, c.name, tc.tournamentName " +
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
        String round = r[0] != null ? r[0].toString() : "Match";
        LocalDateTime when = (LocalDateTime) r[1];
        String teamA = r[2] != null ? (String) r[2] : "TBD";
        String teamB = r[3] != null ? (String) r[3] : "TBD";
        String venue = r[4] != null ? (String) r[4] : "TBA";
        String court = r[6] != null ? (String) r[6] : "";
        String tournament = (String) r[7];

        String maskedPhone = maskPhone(user.getPhone());

        if (!Boolean.TRUE.equals(confirmed)) {
            return Map.of("requires_confirmation", true,
                    "match", teamA + " vs " + teamB,
                    "channel", channel, "phone", maskedPhone,
                    "message", "Send match details via " + channel + " to " + maskedPhone + "?");
        }

        String body;
        if ("WHATSAPP".equalsIgnoreCase(channel)) {
            body = "🏆 *" + tournament + "* — " + round + "\n\n"
                    + "⚔️ *" + teamA + " vs " + teamB + "*\n"
                    + "🗓 " + (when != null ? when.format(DISPLAY_FMT) : "TBA") + "\n"
                    + "📍 " + venue + (!court.isEmpty() ? " — " + court : "") + "\n\n"
                    + "_Good luck!_ 🎯";
        } else {
            body = tournament + " — " + round + "\n"
                    + teamA + " vs " + teamB + "\n"
                    + (when != null ? when.format(DISPLAY_FMT) : "TBA") + "\n"
                    + venue + (!court.isEmpty() ? " — " + court : "");
        }

        SmsResult result = "WHATSAPP".equalsIgnoreCase(channel)
                ? smsService.sendWhatsApp(user.getPhone(), body)
                : smsService.sendSms(user.getPhone(), body);

        return Map.of("success", result.success(), "channel", channel, "phone", maskedPhone);
    }

    // ── WEBSOCKET PUSH ──────────────────────────────────────────────────

    @Tool(description = "Send a real-time push notification to the user's connected mobile/web app "
            + "about an upcoming match. The notification appears instantly as a popup or banner "
            + "if the user has the app open. Does not require confirmation.")
    public Object pushMatchReminderToDevice(
            @ToolParam(description = "Match ID") Long matchId) {

        UserContext ctx = AgentSecurityContext.get();

        var rows = em.createQuery(
                        "SELECT m.scheduledAt, ta.teamName, tb.teamName, v.name " +
                        "FROM TournamentMatch m LEFT JOIN m.teamA ta LEFT JOIN m.teamB tb " +
                        "LEFT JOIN m.venue v JOIN m.config tc " +
                        "WHERE m.id = :mid AND tc.community.id = :comId", Object[].class)
                .setParameter("mid", matchId)
                .setParameter("comId", ctx.communityId())
                .getResultList();

        if (rows.isEmpty()) return Map.of("error", "Match not found in your community");

        Object[] r = rows.get(0);
        LocalDateTime when = (LocalDateTime) r[0];
        String teamA = r[1] != null ? (String) r[1] : "TBD";
        String teamB = r[2] != null ? (String) r[2] : "TBD";
        String venue = r[3] != null ? (String) r[3] : "TBA";

        webSocketPush.pushMatchReminder(ctx.userId(), teamA, teamB,
                when != null ? when.format(DISPLAY_FMT) : "TBA", venue, matchId);

        return Map.of("success", true,
                "message", "Push notification sent to your device for " + teamA + " vs " + teamB);
    }

    @Tool(description = "Send a real-time push notification to the user's device with a custom "
            + "message — useful for schedule updates, results, or reminders. "
            + "Only works if the user has the app open.")
    public Object pushCustomNotification(
            @ToolParam(description = "Notification title") String title,
            @ToolParam(description = "Notification body") String body) {

        UserContext ctx = AgentSecurityContext.get();
        webSocketPush.pushAiAlert(ctx.userId(), title, body);

        return Map.of("success", true, "message", "Push notification sent: " + title);
    }

    // ── HELPERS ─────────────────────────────────────────────────────────

    private record MatchInfo(
            Long matchId, String round, LocalDateTime scheduledAt,
            String teamA, String teamB, String venue, String court,
            String tournament) {}

    private List<MatchInfo> getUpcomingMatches(UserContext ctx) {
        return em.createQuery(
                        "SELECT m.id, m.round, m.scheduledAt, ta.teamName, tb.teamName, " +
                        "v.name, c.name, tc.tournamentName " +
                        "FROM TournamentMatch m " +
                        "LEFT JOIN m.teamA ta LEFT JOIN m.teamB tb " +
                        "LEFT JOIN m.venue v LEFT JOIN m.court c " +
                        "JOIN m.config tc " +
                        "WHERE tc.community.id = :comId " +
                        "AND m.status IN ('SCHEDULED', 'PUBLISHED') " +
                        "AND (ta.ownerUser.id = :uid OR tb.ownerUser.id = :uid " +
                        "OR ta.captainUser.id = :uid OR tb.captainUser.id = :uid " +
                        "OR ta.id IN (SELECT ap.assignedTeam.id FROM AuctionPlayer ap WHERE ap.user.id = :uid) " +
                        "OR tb.id IN (SELECT ap2.assignedTeam.id FROM AuctionPlayer ap2 WHERE ap2.user.id = :uid)) " +
                        "ORDER BY m.scheduledAt ASC", Object[].class)
                .setParameter("comId", ctx.communityId())
                .setParameter("uid", ctx.userId())
                .setMaxResults(15)
                .getResultList().stream()
                .map(r -> new MatchInfo(
                        (Long) r[0],
                        r[1] != null ? r[1].toString() : "Match",
                        (LocalDateTime) r[2],
                        r[3] != null ? (String) r[3] : "TBD",
                        r[4] != null ? (String) r[4] : "TBD",
                        r[5] != null ? (String) r[5] : "TBA",
                        r[6] != null ? (String) r[6] : "",
                        r[7] != null ? (String) r[7] : "Tournament"))
                .collect(Collectors.toList());
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}
