package com.manacommunity.api.ai.tool;

import com.manacommunity.api.ai.config.AgentSecurityContext;
import com.manacommunity.api.ai.config.AgentSecurityContext.UserContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Spring AI tool methods for tournament, match, and standings queries.
 *
 * <p>All queries are scoped to the user's community via
 * {@code se.community.id = :communityId} (where {@code se} is the linked SportsEvent).</p>
 *
 * <p>Entity relationships:</p>
 * <ul>
 *   <li>{@code Tournament} ↔ {@code SportsEvent} (many-to-many via {@code tournament_sports_events})</li>
 *   <li>{@code TournamentConfig} → {@code Tournament}</li>
 *   <li>{@code TournamentMatch} → {@code TournamentConfig}, {@code AuctionTeam} (teamA/teamB),
 *       {@code Venue}, {@code Court}</li>
 *   <li>{@code GroupTeamStanding} → {@code TournamentGroup}, {@code AuctionTeam}</li>
 * </ul>
 */
@Slf4j
@Component
@Transactional(readOnly = true)
public class TournamentQueryTools {

    @PersistenceContext
    private EntityManager em;

    // ── READ TOOLS ─────────────────────────────────────────────────────

    @Tool(description = "List tournaments in the user's community. Read-only. "
            + "Returns tournament ID, name, registration status, and linked event info.")
    public List<Map<String, Object>> listCommunityTournaments() {
        UserContext ctx = AgentSecurityContext.get();
        log.debug("listCommunityTournaments: community={}", ctx.communityId());

        return em.createQuery(
                        "SELECT DISTINCT t.id, t.name, t.registrationStatus, " +
                        "se.id, se.name, se.eventDateStart " +
                        "FROM Tournament t JOIN t.sportsEvents se " +
                        "WHERE se.community.id = :comId " +
                        "ORDER BY se.eventDateStart DESC", Object[].class)
                .setParameter("comId", ctx.communityId())
                .getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("tournament_id", r[0]);
                    m.put("tournament_name", r[1]);
                    m.put("registration_status", r[2] != null ? r[2].toString() : null);
                    m.put("event_id", r[3]);
                    m.put("event_name", r[4]);
                    m.put("event_date", r[5] != null ? r[5].toString() : null);
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "Get upcoming matches for a tournament or for the current user. "
            + "Community-scoped, read-only. Shows teams, venue, court, scheduled time, and status.")
    public Object getUpcomingMatches(
            @ToolParam(required = false, description = "Tournament config ID (optional — omit to see all)")
            Long tournamentConfigId,
            @ToolParam(required = false, description = "Only show the current user's matches")
            Boolean myMatchesOnly,
            @ToolParam(required = false, description = "Max results (default 10)")
            Integer limit) {

        UserContext ctx = AgentSecurityContext.get();

        StringBuilder jpql = new StringBuilder(
                "SELECT m.id, m.round, m.roundNumber, m.matchNumber, m.scheduledAt, " +
                "m.durationMinutes, m.status, " +
                "ta.teamName, tb.teamName, " +
                "v.name, c.name " +
                "FROM TournamentMatch m " +
                "LEFT JOIN m.teamA ta LEFT JOIN m.teamB tb " +
                "LEFT JOIN m.venue v LEFT JOIN m.court c " +
                "JOIN m.config tc JOIN tc.tournament t JOIN t.sportsEvents se " +
                "WHERE se.community.id = :comId " +
                "AND m.status IN ('SCHEDULED', 'IN_PROGRESS')");

        if (tournamentConfigId != null) {
            jpql.append(" AND tc.id = :tcId");
        }
        if (Boolean.TRUE.equals(myMatchesOnly)) {
            jpql.append(" AND (ta.ownerUser.id = :uid OR tb.ownerUser.id = :uid " +
                         "OR ta.captainUser.id = :uid OR tb.captainUser.id = :uid)");
        }
        jpql.append(" ORDER BY m.scheduledAt ASC");

        var query = em.createQuery(jpql.toString(), Object[].class)
                .setParameter("comId", ctx.communityId());

        if (tournamentConfigId != null) query.setParameter("tcId", tournamentConfigId);
        if (Boolean.TRUE.equals(myMatchesOnly)) query.setParameter("uid", ctx.userId());
        query.setMaxResults(limit != null ? limit : 10);

        return query.getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("match_id", r[0]);
                    m.put("round", r[1] != null ? r[1].toString() : null);
                    m.put("round_number", r[2]);
                    m.put("match_number", r[3]);
                    m.put("scheduled_at", r[4] != null ? r[4].toString() : null);
                    m.put("duration_minutes", r[5]);
                    m.put("status", r[6] != null ? r[6].toString() : null);
                    m.put("team_a", r[7]);
                    m.put("team_b", r[8]);
                    m.put("venue", r[9]);
                    m.put("court", r[10]);
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "Get match results — completed matches with scores and winners. "
            + "Community-scoped, read-only.")
    public Object getMatchResults(
            @ToolParam(required = false, description = "Tournament config ID (optional)")
            Long tournamentConfigId,
            @ToolParam(required = false, description = "Team name filter")
            String teamName,
            @ToolParam(required = false, description = "Max results (default 10)")
            Integer limit) {

        UserContext ctx = AgentSecurityContext.get();

        StringBuilder jpql = new StringBuilder(
                "SELECT m.id, m.round, m.matchNumber, m.scheduledAt, " +
                "ta.teamName, tb.teamName, " +
                "m.scoreTeamA, m.scoreTeamB, m.status " +
                "FROM TournamentMatch m " +
                "LEFT JOIN m.teamA ta LEFT JOIN m.teamB tb " +
                "JOIN m.config tc JOIN tc.tournament t JOIN t.sportsEvents se " +
                "WHERE se.community.id = :comId " +
                "AND m.status = 'COMPLETED'");

        if (tournamentConfigId != null) jpql.append(" AND tc.id = :tcId");
        if (teamName != null) {
            jpql.append(" AND (LOWER(ta.teamName) LIKE LOWER(:tn) OR LOWER(tb.teamName) LIKE LOWER(:tn))");
        }
        jpql.append(" ORDER BY m.scheduledAt DESC");

        var query = em.createQuery(jpql.toString(), Object[].class)
                .setParameter("comId", ctx.communityId());

        if (tournamentConfigId != null) query.setParameter("tcId", tournamentConfigId);
        if (teamName != null) query.setParameter("tn", "%" + teamName + "%");
        query.setMaxResults(limit != null ? limit : 10);

        return query.getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("match_id", r[0]);
                    m.put("round", r[1] != null ? r[1].toString() : null);
                    m.put("match_number", r[2]);
                    m.put("played_at", r[3] != null ? r[3].toString() : null);
                    m.put("team_a", r[4]);
                    m.put("team_b", r[5]);
                    m.put("score_team_a", r[6]);
                    m.put("score_team_b", r[7]);
                    m.put("status", r[8] != null ? r[8].toString() : null);
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "Get group standings for a tournament — points table with wins, losses, "
            + "and net run rate. Community-scoped, read-only.")
    public Object getGroupStandings(
            @ToolParam(description = "Tournament config ID") Long tournamentConfigId) {

        UserContext ctx = AgentSecurityContext.get();

        // Verify tournament belongs to user's community
        Long count = em.createQuery(
                        "SELECT COUNT(tc) FROM TournamentConfig tc " +
                        "JOIN tc.tournament t JOIN t.sportsEvents se " +
                        "WHERE tc.id = :tcId AND se.community.id = :comId", Long.class)
                .setParameter("tcId", tournamentConfigId)
                .setParameter("comId", ctx.communityId())
                .getSingleResult();

        if (count == 0) return Map.of("error", "Tournament not found in your community");

        return em.createQuery(
                        "SELECT g.name, s.rank, t.teamName, s.played, s.won, s.lost, s.drawn, " +
                        "s.points, s.netRunRate " +
                        "FROM GroupTeamStanding s " +
                        "JOIN s.group g JOIN s.team t " +
                        "WHERE g.config.id = :tcId " +
                        "ORDER BY g.name, s.rank ASC", Object[].class)
                .setParameter("tcId", tournamentConfigId)
                .getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("group", r[0]);
                    m.put("rank", r[1]);
                    m.put("team", r[2]);
                    m.put("played", r[3]);
                    m.put("won", r[4]);
                    m.put("lost", r[5]);
                    m.put("drawn", r[6]);
                    m.put("points", r[7]);
                    m.put("net_run_rate", r[8]);
                    return m;
                })
                .collect(Collectors.toList());
    }
}
