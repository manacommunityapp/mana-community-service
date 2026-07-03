package com.manacommunity.api.ai.tool;

import com.manacommunity.api.model.Community;

import com.manacommunity.api.ai.config.AgentSecurityContext;
import com.manacommunity.api.ai.config.AgentSecurityContext.UserContext;
import com.manacommunity.api.email.EmailMessage;
import com.manacommunity.api.email.EmailService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI tools for generating post-tournament summary reports — champion, runner-up,
 * group standings, match results, top performers, and stats.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentReportTools {

    @PersistenceContext
    private EntityManager em;

    private final EmailService emailService;
    private final AppUserRepository userRepository;

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Tool(description = "Generate a comprehensive post-tournament summary report. Includes: "
            + "champion, runner-up, group standings, all match results with scores, "
            + "match stats (total matches, highest score, closest match), and team records. "
            + "Community-scoped, read-only.")
    public Object generateTournamentReport(
            @ToolParam(description = "Tournament config ID") Long configId) {

        UserContext ctx = AgentSecurityContext.get();

        // Verify community access
        var configRows = em.createQuery(
                "SELECT tc.tournamentName, tc.tournamentType, tc.totalTeams, tc.status, " +
                "tc.startDate, tc.endDate, s.name " +
                "FROM TournamentConfig tc LEFT JOIN tc.sport s " +
                "WHERE tc.id = :tcId AND tc.community.id = :comId", Object[].class)
                .setParameter("tcId", configId)
                .setParameter("comId", ctx.communityId())
                .getResultList();

        if (configRows.isEmpty()) return Map.of("error", "Tournament not found in your community");

        Object[] cfg = configRows.get(0);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("tournament_name", cfg[0]);
        report.put("type", cfg[1] != null ? cfg[1].toString() : null);
        report.put("total_teams", cfg[2]);
        report.put("status", cfg[3] != null ? cfg[3].toString() : null);
        report.put("start_date", cfg[4] != null ? cfg[4].toString() : null);
        report.put("end_date", cfg[5] != null ? cfg[5].toString() : null);
        report.put("sport", cfg[6]);

        // Match stats
        var matchStats = em.createQuery(
                "SELECT COUNT(m), " +
                "SUM(CASE WHEN m.status = 'COMPLETED' THEN 1 ELSE 0 END), " +
                "MAX(m.scoreTeamA + m.scoreTeamB) " +
                "FROM TournamentMatch m WHERE m.config.id = :tcId", Object[].class)
                .setParameter("tcId", configId)
                .getResultList();

        if (!matchStats.isEmpty()) {
            Object[] ms = matchStats.get(0);
            report.put("total_matches", ms[0]);
            report.put("completed_matches", ms[1]);
        }

        // Final / championship match
        var finalMatch = em.createQuery(
                "SELECT ta.teamName, tb.teamName, m.scoreTeamA, m.scoreTeamB, m.scheduledAt " +
                "FROM TournamentMatch m LEFT JOIN m.teamA ta LEFT JOIN m.teamB tb " +
                "WHERE m.config.id = :tcId AND m.round = 'FINAL' AND m.status = 'COMPLETED'",
                Object[].class)
                .setParameter("tcId", configId)
                .getResultList();

        if (!finalMatch.isEmpty()) {
            Object[] f = finalMatch.get(0);
            int scoreA = f[2] != null ? (Integer) f[2] : 0;
            int scoreB = f[3] != null ? (Integer) f[3] : 0;
            String champion = scoreA > scoreB ? (String) f[0] : (String) f[1];
            String runnerUp = scoreA > scoreB ? (String) f[1] : (String) f[0];

            report.put("champion", champion);
            report.put("runner_up", runnerUp);
            report.put("final_score", f[0] + " " + scoreA + " - " + scoreB + " " + f[1]);
        }

        // Group standings
        var standings = em.createQuery(
                "SELECT g.name, s.rank, t.teamName, s.played, s.won, s.lost, " +
                "s.drawn, s.points, s.netRunRate " +
                "FROM GroupTeamStanding s JOIN s.group g JOIN s.team t " +
                "WHERE g.config.id = :tcId ORDER BY g.name, s.rank", Object[].class)
                .setParameter("tcId", configId)
                .getResultList();

        if (!standings.isEmpty()) {
            Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();
            for (Object[] s : standings) {
                String groupName = (String) s[0];
                Map<String, Object> team = new LinkedHashMap<>();
                team.put("rank", s[1]);
                team.put("team", s[2]);
                team.put("played", s[3]);
                team.put("won", s[4]);
                team.put("lost", s[5]);
                team.put("drawn", s[6]);
                team.put("points", s[7]);
                team.put("nrr", s[8]);
                groups.computeIfAbsent(groupName, k -> new ArrayList<>()).add(team);
            }
            report.put("group_standings", groups);
        }

        // All completed match results
        var results = em.createQuery(
                "SELECT m.round, m.matchNumber, ta.teamName, tb.teamName, " +
                "m.scoreTeamA, m.scoreTeamB, m.scheduledAt " +
                "FROM TournamentMatch m LEFT JOIN m.teamA ta LEFT JOIN m.teamB tb " +
                "WHERE m.config.id = :tcId AND m.status = 'COMPLETED' " +
                "ORDER BY m.scheduledAt ASC", Object[].class)
                .setParameter("tcId", configId)
                .getResultList();

        report.put("match_results", results.stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("round", r[0] != null ? r[0].toString() : null);
                    m.put("match_no", r[1]);
                    m.put("teams", r[2] + " vs " + r[3]);
                    m.put("score", r[4] + " - " + r[5]);
                    int sA = r[4] != null ? (Integer) r[4] : 0;
                    int sB = r[5] != null ? (Integer) r[5] : 0;
                    m.put("winner", sA > sB ? r[2] : (sB > sA ? r[3] : "Draw"));
                    return m;
                })
                .collect(Collectors.toList()));

        // Team records
        var teamRecords = em.createQuery(
                "SELECT t.teamName, " +
                "SUM(CASE WHEN " +
                "  (m.teamA.id = t.id AND m.scoreTeamA > m.scoreTeamB) OR " +
                "  (m.teamB.id = t.id AND m.scoreTeamB > m.scoreTeamA) THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN " +
                "  (m.teamA.id = t.id AND m.scoreTeamA < m.scoreTeamB) OR " +
                "  (m.teamB.id = t.id AND m.scoreTeamB < m.scoreTeamA) THEN 1 ELSE 0 END), " +
                "COUNT(m) " +
                "FROM TournamentMatch m, AuctionTeam t " +
                "WHERE m.config.id = :tcId AND m.status = 'COMPLETED' " +
                "AND (m.teamA.id = t.id OR m.teamB.id = t.id) " +
                "GROUP BY t.teamName ORDER BY " +
                "SUM(CASE WHEN (m.teamA.id = t.id AND m.scoreTeamA > m.scoreTeamB) OR " +
                "(m.teamB.id = t.id AND m.scoreTeamB > m.scoreTeamA) THEN 1 ELSE 0 END) DESC",
                Object[].class)
                .setParameter("tcId", configId)
                .getResultList();

        report.put("team_records", teamRecords.stream()
                .map(r -> Map.of("team", r[0], "wins", r[1], "losses", r[2], "played", r[3]))
                .collect(Collectors.toList()));

        return report;
    }

    @Tool(description = "Email the tournament report to the current user. Compiles results "
            + "into a formatted HTML email. Requires confirmation.")
    @Transactional
    public Object emailTournamentReport(
            @ToolParam(description = "Tournament config ID") Long configId,
            @ToolParam(description = "Has the user explicitly confirmed?") Boolean confirmed) {

        UserContext ctx = AgentSecurityContext.get();
        AppUser user = userRepository.findById(ctx.userId()).orElse(null);
        if (user == null || user.getEmail() == null) {
            return Map.of("error", "No email address on your account.");
        }

        if (!Boolean.TRUE.equals(confirmed)) {
            return Map.of("requires_confirmation", true,
                    "email", user.getEmail(),
                    "message", "Send tournament report to " + user.getEmail() + "?");
        }

        // Get report data
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) generateTournamentReport(configId);
        if (report.containsKey("error")) return report;

        // Build email
        StringBuilder html = new StringBuilder();
        html.append("<div style='font-family:Arial;max-width:600px;margin:0 auto;'>");
        html.append("<h1 style='color:#2563eb;'>🏆 ").append(report.get("tournament_name")).append("</h1>");
        html.append("<p>").append(report.get("sport")).append(" — ").append(report.get("type")).append("</p>");

        if (report.containsKey("champion")) {
            html.append("<div style='background:#fef3c7;padding:16px;border-radius:8px;margin:16px 0;text-align:center;'>");
            html.append("<div style='font-size:14px;color:#92400e;'>CHAMPION</div>");
            html.append("<div style='font-size:24px;font-weight:bold;'>🏆 ").append(report.get("champion")).append("</div>");
            html.append("<div style='margin-top:8px;'>Final: ").append(report.get("final_score")).append("</div>");
            html.append("<div>Runner-up: ").append(report.get("runner_up")).append("</div>");
            html.append("</div>");
        }

        html.append("<p><strong>Matches:</strong> ").append(report.get("completed_matches"));
        html.append(" / ").append(report.get("total_matches")).append(" completed</p>");
        html.append("<p>Full report available in the app.</p>");
        html.append("<p style='color:#94a3b8;font-size:12px;'>— Mana Community AI Assistant</p>");
        html.append("</div>");

        emailService.send(new EmailMessage(
                user.getEmail(), user.getFullName(),
                "🏆 " + report.get("tournament_name") + " — Tournament Report",
                html.toString()));

        log.info("Tournament report emailed for config={} to user={}", configId, ctx.userId());
        return Map.of("success", true, "email", user.getEmail());
    }
}
