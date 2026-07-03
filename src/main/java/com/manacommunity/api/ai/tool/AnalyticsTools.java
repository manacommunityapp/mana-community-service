package com.manacommunity.api.ai.tool;

import com.manacommunity.api.model.Community;

import com.manacommunity.api.user.model.UserProfile;

import com.manacommunity.api.user.model.AppUser;

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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI tools for sports analytics — team head-to-head records, venue utilization,
 * player profiles, and schedule conflict detection.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
@Transactional(readOnly = true)
public class AnalyticsTools {

    @PersistenceContext
    private EntityManager em;

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ── HEAD-TO-HEAD ───────────────────────────────────────────────────

    @Tool(description = "Compare two teams' head-to-head record across all completed matches. "
            + "Shows wins, losses, draws, match history with scores, and win rate. "
            + "Community-scoped, read-only.")
    public Object getHeadToHead(
            @ToolParam(description = "First team name (partial match)") String teamAName,
            @ToolParam(description = "Second team name (partial match)") String teamBName) {

        UserContext ctx = AgentSecurityContext.get();

        // Resolve team IDs
        Long teamAId = resolveTeamId(ctx, teamAName);
        Long teamBId = resolveTeamId(ctx, teamBName);

        if (teamAId == null) return Map.of("error", "Team '" + teamAName + "' not found");
        if (teamBId == null) return Map.of("error", "Team '" + teamBName + "' not found");

        // Get team names
        String teamA = getTeamName(teamAId);
        String teamB = getTeamName(teamBId);

        // Find all completed matches between these two teams
        var matches = em.createQuery(
                "SELECT m.id, m.round, m.scheduledAt, m.scoreTeamA, m.scoreTeamB, " +
                "m.teamA.id, m.teamB.id, tc.tournamentName " +
                "FROM TournamentMatch m JOIN m.config tc " +
                "WHERE m.status = 'COMPLETED' " +
                "AND tc.community.id = :comId " +
                "AND ((m.teamA.id = :tA AND m.teamB.id = :tB) " +
                "  OR (m.teamA.id = :tB AND m.teamB.id = :tA)) " +
                "ORDER BY m.scheduledAt DESC", Object[].class)
                .setParameter("comId", ctx.communityId())
                .setParameter("tA", teamAId)
                .setParameter("tB", teamBId)
                .getResultList();

        int winsA = 0, winsB = 0, draws = 0;
        List<Map<String, Object>> history = new ArrayList<>();

        for (Object[] m : matches) {
            Integer scoreA = (Integer) m[3];
            Integer scoreB = (Integer) m[4];
            Long matchTeamAId = (Long) m[5];

            // Normalize scores relative to teamA/teamB order
            int scoreForA, scoreForB;
            if (matchTeamAId.equals(teamAId)) {
                scoreForA = scoreA != null ? scoreA : 0;
                scoreForB = scoreB != null ? scoreB : 0;
            } else {
                scoreForA = scoreB != null ? scoreB : 0;
                scoreForB = scoreA != null ? scoreA : 0;
            }

            String winner;
            if (scoreForA > scoreForB) { winsA++; winner = teamA; }
            else if (scoreForB > scoreForA) { winsB++; winner = teamB; }
            else { draws++; winner = "Draw"; }

            Map<String, Object> hm = new LinkedHashMap<>();
            hm.put("tournament", m[7]);
            hm.put("round", m[1] != null ? m[1].toString() : null);
            hm.put("date", m[2] != null ? ((LocalDateTime) m[2]).format(DISPLAY) : null);
            hm.put(teamA + "_score", scoreForA);
            hm.put(teamB + "_score", scoreForB);
            hm.put("winner", winner);
            history.add(hm);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("team_a", teamA);
        result.put("team_b", teamB);
        result.put("total_matches", matches.size());
        result.put(teamA + "_wins", winsA);
        result.put(teamB + "_wins", winsB);
        result.put("draws", draws);
        result.put(teamA + "_win_rate", matches.size() > 0
                ? Math.round((double) winsA / matches.size() * 100) + "%" : "N/A");
        result.put("match_history", history);

        return result;
    }

    @Tool(description = "Get a team's overall tournament record — wins, losses, draws, "
            + "total matches, and recent form. Community-scoped, read-only.")
    public Object getTeamRecord(
            @ToolParam(description = "Team name (partial match)") String teamName) {

        UserContext ctx = AgentSecurityContext.get();
        Long teamId = resolveTeamId(ctx, teamName);
        if (teamId == null) return Map.of("error", "Team not found");

        String name = getTeamName(teamId);

        var matches = em.createQuery(
                "SELECT m.teamA.id, m.scoreTeamA, m.scoreTeamB, m.scheduledAt " +
                "FROM TournamentMatch m JOIN m.config tc " +
                "WHERE m.status = 'COMPLETED' AND tc.community.id = :comId " +
                "AND (m.teamA.id = :tid OR m.teamB.id = :tid) " +
                "ORDER BY m.scheduledAt DESC", Object[].class)
                .setParameter("comId", ctx.communityId())
                .setParameter("tid", teamId)
                .getResultList();

        int wins = 0, losses = 0, draws = 0;
        List<String> recentForm = new ArrayList<>();

        for (Object[] m : matches) {
            boolean isTeamA = teamId.equals(m[0]);
            int myScore = isTeamA ? ((Integer) m[1] != null ? (Integer) m[1] : 0)
                    : ((Integer) m[2] != null ? (Integer) m[2] : 0);
            int oppScore = isTeamA ? ((Integer) m[2] != null ? (Integer) m[2] : 0)
                    : ((Integer) m[1] != null ? (Integer) m[1] : 0);

            if (myScore > oppScore) { wins++; if (recentForm.size() < 5) recentForm.add("W"); }
            else if (oppScore > myScore) { losses++; if (recentForm.size() < 5) recentForm.add("L"); }
            else { draws++; if (recentForm.size() < 5) recentForm.add("D"); }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("team", name);
        result.put("total_matches", matches.size());
        result.put("wins", wins);
        result.put("losses", losses);
        result.put("draws", draws);
        result.put("win_rate", matches.size() > 0
                ? Math.round((double) wins / matches.size() * 100) + "%" : "N/A");
        result.put("recent_form", String.join("-", recentForm));
        return result;
    }

    // ── VENUE UTILIZATION ──────────────────────────────────────────────

    @Tool(description = "[ADMIN] Venue and court utilization analytics — which courts are "
            + "busiest, idle slots, matches by day of week, and peak hours. Read-only.")
    public Object getVenueUtilization(
            @ToolParam(required = false, description = "Venue ID (omit for all venues)") Long venueId,
            @ToolParam(required = false, description = "Tournament config ID filter") Long configId) {

        UserContext ctx = AgentSecurityContext.get();

        StringBuilder where = new StringBuilder(
                "FROM TournamentMatch m LEFT JOIN m.venue v LEFT JOIN m.court c " +
                "JOIN m.config tc WHERE tc.community.id = :comId " +
                "AND m.scheduledAt IS NOT NULL");

        if (venueId != null) where.append(" AND v.id = :vid");
        if (configId != null) where.append(" AND tc.id = :cid");

        Map<String, Object> result = new LinkedHashMap<>();

        // By court
        var byCourt = em.createQuery(
                "SELECT COALESCE(v.name, 'Unassigned'), COALESCE(c.name, 'Unassigned'), COUNT(m) " +
                where + " GROUP BY v.name, c.name ORDER BY COUNT(m) DESC", Object[].class)
                .setParameter("comId", ctx.communityId());
        if (venueId != null) byCourt.setParameter("vid", venueId);
        if (configId != null) byCourt.setParameter("cid", configId);

        result.put("by_court", byCourt.getResultList().stream()
                .map(r -> Map.of("venue", r[0], "court", r[1], "matches", r[2]))
                .collect(Collectors.toList()));

        // By day of week
        var byDay = em.createQuery(
                "SELECT FUNCTION('DAYOFWEEK', m.scheduledAt), COUNT(m) " +
                where + " GROUP BY FUNCTION('DAYOFWEEK', m.scheduledAt) " +
                "ORDER BY FUNCTION('DAYOFWEEK', m.scheduledAt)", Object[].class)
                .setParameter("comId", ctx.communityId());
        if (venueId != null) byDay.setParameter("vid", venueId);
        if (configId != null) byDay.setParameter("cid", configId);

        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        result.put("by_day_of_week", byDay.getResultList().stream()
                .map(r -> {
                    int dayIdx = ((Number) r[0]).intValue() - 1;
                    return Map.of("day", dayIdx >= 0 && dayIdx < 7 ? days[dayIdx] : "?",
                            "matches", r[1]);
                })
                .collect(Collectors.toList()));

        // By hour of day
        var byHour = em.createQuery(
                "SELECT FUNCTION('HOUR', m.scheduledAt), COUNT(m) " +
                where + " GROUP BY FUNCTION('HOUR', m.scheduledAt) " +
                "ORDER BY FUNCTION('HOUR', m.scheduledAt)", Object[].class)
                .setParameter("comId", ctx.communityId());
        if (venueId != null) byHour.setParameter("vid", venueId);
        if (configId != null) byHour.setParameter("cid", configId);

        result.put("by_hour", byHour.getResultList().stream()
                .map(r -> Map.of("hour", r[0] + ":00", "matches", r[1]))
                .collect(Collectors.toList()));

        // Total match hours
        var totalHours = em.createQuery(
                "SELECT SUM(m.durationMinutes) " + where, Long.class)
                .setParameter("comId", ctx.communityId());
        if (venueId != null) totalHours.setParameter("vid", venueId);
        if (configId != null) totalHours.setParameter("cid", configId);
        Long mins = totalHours.getSingleResult();
        result.put("total_court_hours", mins != null ? mins / 60.0 : 0);

        return result;
    }

    // ── PLAYER PROFILE ─────────────────────────────────────────────────

    @Tool(description = "Get a player's profile card — bio, skills, team, auction price, "
            + "match history stats, events attended, and posts count. Community-scoped.")
    public Object getPlayerProfile(
            @ToolParam(description = "Player name (partial match)") String playerName) {

        UserContext ctx = AgentSecurityContext.get();

        // Find user
        var users = em.createQuery(
                "SELECT u.id, u.fullName, u.email, u.phone, u.role, u.dateOfBirth, " +
                "u.gender, u.kycStatus, u.profilePicUrl " +
                "FROM AppUser u WHERE u.community.id = :comId " +
                "AND LOWER(u.fullName) LIKE LOWER(:nm) AND u.isActive = true", Object[].class)
                .setParameter("comId", ctx.communityId())
                .setParameter("nm", "%" + playerName + "%")
                .setMaxResults(1)
                .getResultList();

        if (users.isEmpty()) return Map.of("error", "Player not found in your community");

        Object[] u = users.get(0);
        Long userId = (Long) u[0];
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", u[1]);
        result.put("role", u[4]);
        result.put("gender", u[6]);
        result.put("kyc_status", u[7]);

        // Profile
        var profiles = em.createQuery(
                "SELECT p.bio, p.skills, p.posts, p.connections, p.eventsAttended, p.sportsPlayed " +
                "FROM UserProfile p WHERE p.user.id = :uid", Object[].class)
                .setParameter("uid", userId)
                .getResultList();

        if (!profiles.isEmpty()) {
            Object[] p = profiles.get(0);
            result.put("bio", p[0]);
            result.put("skills", p[1]);
            result.put("posts_count", p[2]);
            result.put("connections", p[3]);
            result.put("events_attended", p[4]);
            result.put("sports_played", p[5]);
        }

        // Auction info
        var auctions = em.createQuery(
                "SELECT ap.playerName, ap.category, ap.playerRole, ap.basePrice, " +
                "ap.soldPrice, ap.status, t.teamName, ac.seasonName " +
                "FROM AuctionPlayer ap LEFT JOIN ap.assignedTeam t JOIN ap.config ac " +
                "WHERE ap.user.id = :uid AND ac.createdBy.community.id = :comId " +
                "ORDER BY ac.id DESC", Object[].class)
                .setParameter("uid", userId)
                .setParameter("comId", ctx.communityId())
                .setMaxResults(3)
                .getResultList();

        result.put("auction_history", auctions.stream()
                .map(a -> Map.of("season", a[7], "category", a[1],
                        "role", a[2] != null ? a[2] : "",
                        "base_price", a[3], "sold_price", a[4],
                        "status", a[5].toString(), "team", a[6] != null ? a[6] : "Unsold"))
                .collect(Collectors.toList()));

        // Match stats
        Long matchesPlayed = em.createQuery(
                "SELECT COUNT(m) FROM TournamentMatch m JOIN m.config tc " +
                "WHERE m.status = 'COMPLETED' AND tc.community.id = :comId " +
                "AND (m.teamA.id IN (SELECT ap.assignedTeam.id FROM AuctionPlayer ap WHERE ap.user.id = :uid) " +
                "OR m.teamB.id IN (SELECT ap2.assignedTeam.id FROM AuctionPlayer ap2 WHERE ap2.user.id = :uid))",
                Long.class)
                .setParameter("uid", userId)
                .setParameter("comId", ctx.communityId())
                .getSingleResult();
        result.put("matches_played", matchesPlayed);

        // Events registered for
        Long eventsRegistered = em.createQuery(
                "SELECT COUNT(r) FROM SportsEventRegistration r " +
                "WHERE r.user.id = :uid AND r.event.community.id = :comId",
                Long.class)
                .setParameter("uid", userId)
                .setParameter("comId", ctx.communityId())
                .getSingleResult();
        result.put("events_registered", eventsRegistered);

        return result;
    }

    // ── SCHEDULE CONFLICT DETECTION ────────────────────────────────────

    @Tool(description = "[ADMIN] Detect scheduling conflicts in a tournament — overlapping "
            + "matches on the same court, same team in two matches at the same time, "
            + "or insufficient break between matches. Read-only.")
    public Object detectScheduleConflicts(
            @ToolParam(description = "Tournament config ID") Long configId) {

        UserContext ctx = AgentSecurityContext.get();

        // Verify community
        Long count = em.createQuery(
                "SELECT COUNT(tc) FROM TournamentConfig tc " +
                "WHERE tc.id = :tcId AND tc.community.id = :comId", Long.class)
                .setParameter("tcId", configId)
                .setParameter("comId", ctx.communityId())
                .getSingleResult();
        if (count == 0) return Map.of("error", "Tournament config not found in your community");

        // Get all scheduled matches
        var matches = em.createQuery(
                "SELECT m.id, m.scheduledAt, m.durationMinutes, " +
                "m.court.id, c.name, m.teamA.id, ta.teamName, m.teamB.id, tb.teamName " +
                "FROM TournamentMatch m " +
                "LEFT JOIN m.court c LEFT JOIN m.teamA ta LEFT JOIN m.teamB tb " +
                "WHERE m.config.id = :tcId AND m.scheduledAt IS NOT NULL " +
                "ORDER BY m.scheduledAt", Object[].class)
                .setParameter("tcId", configId)
                .getResultList();

        List<Map<String, Object>> courtConflicts = new ArrayList<>();
        List<Map<String, Object>> teamConflicts = new ArrayList<>();

        for (int i = 0; i < matches.size(); i++) {
            Object[] m1 = matches.get(i);
            LocalDateTime start1 = (LocalDateTime) m1[1];
            int dur1 = m1[2] != null ? (Integer) m1[2] : 60;
            LocalDateTime end1 = start1.plusMinutes(dur1);

            for (int j = i + 1; j < matches.size(); j++) {
                Object[] m2 = matches.get(j);
                LocalDateTime start2 = (LocalDateTime) m2[1];
                if (start2.isAfter(end1.plusMinutes(30))) break; // optimization

                int dur2 = m2[2] != null ? (Integer) m2[2] : 60;
                LocalDateTime end2 = start2.plusMinutes(dur2);

                boolean overlaps = start1.isBefore(end2) && start2.isBefore(end1);
                if (!overlaps) continue;

                // Court conflict
                if (m1[3] != null && m1[3].equals(m2[3])) {
                    courtConflicts.add(Map.of(
                            "type", "COURT_DOUBLE_BOOKED",
                            "court", m1[4] != null ? m1[4] : "Unknown",
                            "match_1", m1[6] + " vs " + m1[8] + " at " + start1.format(DISPLAY),
                            "match_2", m2[6] + " vs " + m2[8] + " at " + start2.format(DISPLAY),
                            "match_1_id", m1[0], "match_2_id", m2[0]));
                }

                // Team conflict — same team in both matches
                Set<Long> teams1 = new HashSet<>();
                if (m1[5] != null) teams1.add((Long) m1[5]);
                if (m1[7] != null) teams1.add((Long) m1[7]);

                Set<Long> teams2 = new HashSet<>();
                if (m2[5] != null) teams2.add((Long) m2[5]);
                if (m2[7] != null) teams2.add((Long) m2[7]);

                teams1.retainAll(teams2);
                if (!teams1.isEmpty()) {
                    String conflictTeam = getTeamName(teams1.iterator().next());
                    teamConflicts.add(Map.of(
                            "type", "TEAM_DOUBLE_BOOKED",
                            "team", conflictTeam,
                            "match_1", m1[6] + " vs " + m1[8] + " at " + start1.format(DISPLAY),
                            "match_2", m2[6] + " vs " + m2[8] + " at " + start2.format(DISPLAY),
                            "match_1_id", m1[0], "match_2_id", m2[0]));
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_matches_checked", matches.size());
        result.put("court_conflicts", courtConflicts);
        result.put("team_conflicts", teamConflicts);
        result.put("total_conflicts", courtConflicts.size() + teamConflicts.size());
        result.put("clean", courtConflicts.isEmpty() && teamConflicts.isEmpty());

        return result;
    }

    // ── HELPERS ────────────────────────────────────────────────────────

    private Long resolveTeamId(UserContext ctx, String name) {
        var rows = em.createQuery(
                "SELECT t.id FROM AuctionTeam t JOIN t.config c " +
                "WHERE c.createdBy.community.id = :comId " +
                "AND LOWER(t.teamName) LIKE LOWER(:nm)", Long.class)
                .setParameter("comId", ctx.communityId())
                .setParameter("nm", "%" + name + "%")
                .setMaxResults(1)
                .getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String getTeamName(Long teamId) {
        var rows = em.createQuery("SELECT t.teamName FROM AuctionTeam t WHERE t.id = :tid", String.class)
                .setParameter("tid", teamId).getResultList();
        return rows.isEmpty() ? "Unknown" : rows.get(0);
    }
}
