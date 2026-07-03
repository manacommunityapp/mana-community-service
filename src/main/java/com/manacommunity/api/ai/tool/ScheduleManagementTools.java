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

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI tools for tournament scheduling: configuration details, match schedules with
 * venue/court/time assignments, bracket structure, and schedule generation logs.
 *
 * <p>Scoped via {@code TournamentConfig.community.id}.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
@Transactional(readOnly = true)
public class ScheduleManagementTools {

    @PersistenceContext
    private EntityManager em;

    private boolean isConfigInCommunity(Long configId) {
        if (configId == null) return false;
        UserContext ctx = AgentSecurityContext.get();
        return em.createQuery(
                        "SELECT COUNT(tc) FROM TournamentConfig tc " +
                        "WHERE tc.id = :tcId AND tc.community.id = :comId", Long.class)
                .setParameter("tcId", configId)
                .setParameter("comId", ctx.communityId())
                .getSingleResult() > 0;
    }

    // ── READ ────────────────────────────────────────────────────────────

    @Tool(description = "Get full tournament schedule configuration — type, groups, knockout "
            + "settings, points system, timings, venue, and status. Community-scoped, read-only.")
    public Object getTournamentConfig(
            @ToolParam(description = "Tournament config ID") Long configId) {

        if (!isConfigInCommunity(configId)) {
            return Map.of("error", "Tournament config not found in your community");
        }

        var rows = em.createQuery(
                "SELECT tc.id, tc.tournamentName, tc.tournamentType, tc.totalTeams, " +
                "tc.numberOfGroups, tc.teamsPerGroup, tc.teamsAdvancingPerGroup, " +
                "tc.thirdPlaceMatch, tc.hasSeeding, tc.swissRounds, " +
                "tc.startDate, tc.endDate, tc.matchDurationMinutes, tc.breakBetweenMatchesMinutes, " +
                "tc.pointsForWin, tc.pointsForDraw, tc.pointsForLoss, tc.status, " +
                "s.name, v.name, e.name " +
                "FROM TournamentConfig tc " +
                "LEFT JOIN tc.sport s LEFT JOIN tc.venue v LEFT JOIN tc.event e " +
                "WHERE tc.id = :tcId", Object[].class)
                .setParameter("tcId", configId)
                .getResultList();

        if (rows.isEmpty()) return Map.of("error", "Config not found");

        Object[] r = rows.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("config_id", r[0]);
        result.put("tournament_name", r[1]);
        result.put("tournament_type", r[2] != null ? r[2].toString() : null);
        result.put("total_teams", r[3]);
        result.put("number_of_groups", r[4]);
        result.put("teams_per_group", r[5]);
        result.put("teams_advancing_per_group", r[6]);
        result.put("third_place_match", r[7]);
        result.put("has_seeding", r[8]);
        result.put("swiss_rounds", r[9]);
        result.put("start_date", r[10] != null ? r[10].toString() : null);
        result.put("end_date", r[11] != null ? r[11].toString() : null);
        result.put("match_duration_minutes", r[12]);
        result.put("break_between_matches_minutes", r[13]);

        Map<String, Object> points = new LinkedHashMap<>();
        points.put("win", r[14]);
        points.put("draw", r[15]);
        points.put("loss", r[16]);
        result.put("points_system", points);

        result.put("status", r[17] != null ? r[17].toString() : null);
        result.put("sport", r[18]);
        result.put("venue", r[19]);
        result.put("event_name", r[20]);

        // Match and group counts
        Long matchCount = em.createQuery(
                        "SELECT COUNT(m) FROM TournamentMatch m WHERE m.config.id = :tcId",
                        Long.class)
                .setParameter("tcId", configId)
                .getSingleResult();
        result.put("total_matches", matchCount);

        Long groupCount = em.createQuery(
                        "SELECT COUNT(g) FROM TournamentGroup g WHERE g.config.id = :tcId",
                        Long.class)
                .setParameter("tcId", configId)
                .getSingleResult();
        result.put("total_groups", groupCount);

        return result;
    }

    @Tool(description = "List tournament configs in the user's community with status and key settings. "
            + "Read-only.")
    public List<Map<String, Object>> listTournamentConfigs() {
        UserContext ctx = AgentSecurityContext.get();

        return em.createQuery(
                        "SELECT tc.id, tc.tournamentName, tc.tournamentType, tc.totalTeams, " +
                        "tc.status, tc.startDate, s.name, e.name " +
                        "FROM TournamentConfig tc " +
                        "LEFT JOIN tc.sport s LEFT JOIN tc.event e " +
                        "WHERE tc.community.id = :comId " +
                        "ORDER BY tc.createdAt DESC", Object[].class)
                .setParameter("comId", ctx.communityId())
                .getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("config_id", r[0]);
                    m.put("tournament_name", r[1]);
                    m.put("type", r[2] != null ? r[2].toString() : null);
                    m.put("total_teams", r[3]);
                    m.put("status", r[4] != null ? r[4].toString() : null);
                    m.put("start_date", r[5] != null ? r[5].toString() : null);
                    m.put("sport", r[6]);
                    m.put("event", r[7]);
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "Get the full match schedule for a tournament — all matches with teams, "
            + "venue, court, time, round, and status. Ordered chronologically. "
            + "Community-scoped, read-only.")
    public Object getFullSchedule(
            @ToolParam(description = "Tournament config ID") Long configId,
            @ToolParam(required = false, description = "Filter by round: GROUP_STAGE, QUARTER_FINAL, SEMI_FINAL, FINAL, etc.")
            String round,
            @ToolParam(required = false, description = "Filter by status: SCHEDULED, LIVE, COMPLETED, POSTPONED, CANCELLED")
            String status,
            @ToolParam(required = false, description = "Max results (default 25)") Integer limit) {

        if (!isConfigInCommunity(configId)) {
            return Map.of("error", "Tournament config not found in your community");
        }

        StringBuilder jpql = new StringBuilder(
                "SELECT m.id, m.round, m.roundNumber, m.matchNumber, m.bracketSlot, " +
                "m.scheduledAt, m.durationMinutes, m.status, " +
                "ta.teamName, tb.teamName, " +
                "m.scoreTeamA, m.scoreTeamB, " +
                "v.name, c.name, g.name " +
                "FROM TournamentMatch m " +
                "LEFT JOIN m.teamA ta LEFT JOIN m.teamB tb " +
                "LEFT JOIN m.venue v LEFT JOIN m.court c " +
                "LEFT JOIN m.group g " +
                "WHERE m.config.id = :tcId");

        if (round != null) jpql.append(" AND m.round = :rd");
        if (status != null) jpql.append(" AND m.status = :st");
        jpql.append(" ORDER BY m.scheduledAt ASC, m.matchNumber ASC");

        var query = em.createQuery(jpql.toString(), Object[].class)
                .setParameter("tcId", configId);
        if (round != null) query.setParameter("rd",
                com.manacommunity.api.model.scheduler.MatchRound.valueOf(round));
        if (status != null) query.setParameter("st",
                com.manacommunity.api.model.scheduler.MatchStatus.valueOf(status));
        query.setMaxResults(limit != null ? limit : 25);

        return query.getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("match_id", r[0]);
                    m.put("round", r[1] != null ? r[1].toString() : null);
                    m.put("round_number", r[2]);
                    m.put("match_number", r[3]);
                    m.put("bracket_slot", r[4]);
                    m.put("scheduled_at", r[5] != null ? r[5].toString() : null);
                    m.put("duration_minutes", r[6]);
                    m.put("status", r[7] != null ? r[7].toString() : null);
                    m.put("team_a", r[8]);
                    m.put("team_b", r[9]);
                    m.put("score_team_a", r[10]);
                    m.put("score_team_b", r[11]);
                    m.put("venue", r[12]);
                    m.put("court", r[13]);
                    m.put("group", r[14]);
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "Get the knockout bracket structure — match slots showing which teams "
            + "feed into which matches (winner-of-M1 vs winner-of-M2 → M5). "
            + "Community-scoped, read-only.")
    public Object getBracketStructure(
            @ToolParam(description = "Tournament config ID") Long configId) {

        if (!isConfigInCommunity(configId)) {
            return Map.of("error", "Tournament config not found in your community");
        }

        return em.createQuery(
                        "SELECT m.id, m.round, m.roundNumber, m.matchNumber, m.bracketSlot, " +
                        "ta.teamName, tb.teamName, " +
                        "m.winnerFeedFromMatchA, m.winnerFeedFromMatchB, " +
                        "m.winnerAdvancesToMatchId, m.loserSentToMatchId, " +
                        "m.scoreTeamA, m.scoreTeamB, m.status " +
                        "FROM TournamentMatch m " +
                        "LEFT JOIN m.teamA ta LEFT JOIN m.teamB tb " +
                        "WHERE m.config.id = :tcId " +
                        "AND m.round <> 'GROUP_STAGE' " +
                        "ORDER BY m.roundNumber ASC, m.matchNumber ASC", Object[].class)
                .setParameter("tcId", configId)
                .getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("match_id", r[0]);
                    m.put("round", r[1] != null ? r[1].toString() : null);
                    m.put("round_number", r[2]);
                    m.put("match_number", r[3]);
                    m.put("bracket_slot", r[4]);
                    m.put("team_a", r[5] != null ? r[5] : "TBD");
                    m.put("team_b", r[6] != null ? r[6] : "TBD");
                    m.put("feeds_from_match_a", r[7]);
                    m.put("feeds_from_match_b", r[8]);
                    m.put("winner_advances_to", r[9]);
                    m.put("loser_sent_to", r[10]);
                    m.put("score_a", r[11]);
                    m.put("score_b", r[12]);
                    m.put("status", r[13] != null ? r[13].toString() : null);
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "Get group assignments — which teams are in which groups with their "
            + "seed rank. Community-scoped, read-only.")
    public Object getGroupAssignments(
            @ToolParam(description = "Tournament config ID") Long configId) {

        if (!isConfigInCommunity(configId)) {
            return Map.of("error", "Tournament config not found in your community");
        }

        return em.createQuery(
                        "SELECT g.name, t.teamName, s.seedRank " +
                        "FROM GroupTeamStanding s JOIN s.group g JOIN s.team t " +
                        "WHERE g.config.id = :tcId " +
                        "ORDER BY g.name, s.seedRank", Object[].class)
                .setParameter("tcId", configId)
                .getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("group", r[0]);
                    m.put("team", r[1]);
                    m.put("seed_rank", r[2]);
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "Get schedule generation history — logs of when schedules were generated, "
            + "published, or modified. Community-scoped, read-only.")
    public Object getScheduleGenerationLogs(
            @ToolParam(description = "Tournament config ID") Long configId,
            @ToolParam(required = false, description = "Max results (default 10)") Integer limit) {

        if (!isConfigInCommunity(configId)) {
            return Map.of("error", "Tournament config not found in your community");
        }

        return em.createQuery(
                        "SELECT l.id, l.action, l.status, l.message, l.matchesGenerated, " +
                        "l.createdAt, u.fullName " +
                        "FROM ScheduleGenerationLog l LEFT JOIN l.createdBy u " +
                        "WHERE l.config.id = :tcId " +
                        "ORDER BY l.createdAt DESC", Object[].class)
                .setParameter("tcId", configId)
                .setMaxResults(limit != null ? limit : 10)
                .getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("log_id", r[0]);
                    m.put("action", r[1] != null ? r[1].toString() : null);
                    m.put("status", r[2] != null ? r[2].toString() : null);
                    m.put("message", r[3]);
                    m.put("matches_generated", r[4]);
                    m.put("created_at", r[5] != null ? r[5].toString() : null);
                    m.put("created_by", r[6]);
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "Get a schedule summary — match counts by status and round, "
            + "with date range and venue breakdown. Community-scoped, read-only.")
    public Object getScheduleSummary(
            @ToolParam(description = "Tournament config ID") Long configId) {

        if (!isConfigInCommunity(configId)) {
            return Map.of("error", "Tournament config not found in your community");
        }

        Map<String, Object> result = new LinkedHashMap<>();

        // By status
        var byStatus = em.createQuery(
                        "SELECT m.status, COUNT(m) FROM TournamentMatch m " +
                        "WHERE m.config.id = :tcId GROUP BY m.status", Object[].class)
                .setParameter("tcId", configId)
                .getResultList();

        Map<String, Long> statusMap = new LinkedHashMap<>();
        byStatus.forEach(s -> statusMap.put(s[0].toString(), ((Number) s[1]).longValue()));
        result.put("by_status", statusMap);

        // By round
        var byRound = em.createQuery(
                        "SELECT m.round, COUNT(m) FROM TournamentMatch m " +
                        "WHERE m.config.id = :tcId GROUP BY m.round " +
                        "ORDER BY MIN(m.roundNumber)", Object[].class)
                .setParameter("tcId", configId)
                .getResultList();

        Map<String, Long> roundMap = new LinkedHashMap<>();
        byRound.forEach(r -> roundMap.put(r[0].toString(), ((Number) r[1]).longValue()));
        result.put("by_round", roundMap);

        // Date range
        var dateRange = em.createQuery(
                        "SELECT MIN(m.scheduledAt), MAX(m.scheduledAt) FROM TournamentMatch m " +
                        "WHERE m.config.id = :tcId AND m.scheduledAt IS NOT NULL", Object[].class)
                .setParameter("tcId", configId)
                .getResultList();

        if (!dateRange.isEmpty() && dateRange.get(0)[0] != null) {
            result.put("first_match", dateRange.get(0)[0].toString());
            result.put("last_match", dateRange.get(0)[1].toString());
        }

        // By venue
        var byVenue = em.createQuery(
                        "SELECT COALESCE(v.name, 'Unassigned'), COUNT(m) " +
                        "FROM TournamentMatch m LEFT JOIN m.venue v " +
                        "WHERE m.config.id = :tcId GROUP BY v.name", Object[].class)
                .setParameter("tcId", configId)
                .getResultList();

        Map<String, Long> venueMap = new LinkedHashMap<>();
        byVenue.forEach(v -> venueMap.put((String) v[0], ((Number) v[1]).longValue()));
        result.put("by_venue", venueMap);

        return result;
    }
}
