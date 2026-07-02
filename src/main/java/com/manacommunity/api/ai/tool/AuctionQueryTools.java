package com.manacommunity.api.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.ai.config.AgentSecurityContext;
import com.manacommunity.api.ai.config.AgentSecurityContext.UserContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Spring AI tool methods for auction queries.
 *
 * <p>Every query includes {@code c.createdBy.community.id = :communityId} to enforce
 * data isolation. The community ID comes from {@link AgentSecurityContext}, which was
 * validated against the database at request start.</p>
 *
 * <p>Entity field mappings (vs the original broken script):</p>
 * <ul>
 *   <li>{@code AuctionConfig} → community via {@code createdBy.community.id}</li>
 *   <li>{@code AuctionPlayer.config} (ManyToOne), not {@code configId}</li>
 *   <li>{@code AuctionPlayer.status} enum: QUEUED, SELLING, SOLD, PASSED, RETAINED</li>
 *   <li>{@code AuctionTeam.ownerUser} (ManyToOne), not {@code ownerUserId}</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionQueryTools {

    @PersistenceContext
    private EntityManager em;

    private final ObjectMapper objectMapper;

    // ── Community-scoping helper ───────────────────────────────────────

    private boolean isAuctionInCommunity(Long configId) {
        if (configId == null) return false;
        UserContext ctx = AgentSecurityContext.get();
        Long count = em.createQuery(
                        "SELECT COUNT(c) FROM AuctionConfig c " +
                        "WHERE c.id = :cid AND c.createdBy.community.id = :comId", Long.class)
                .setParameter("cid", configId)
                .setParameter("comId", ctx.communityId())
                .getSingleResult();
        return count > 0;
    }

    private Map<String, Object> accessDenied(String reason) {
        return Map.of("error", true, "message", "Access denied: " + reason);
    }

    private Map<String, Object> writeNotAllowed() {
        return Map.of("error", true, "message",
                "Write permission denied. Only SUPER_ADMIN can perform this action.");
    }

    // ── READ TOOLS ─────────────────────────────────────────────────────

    @Tool(description = "List all auctions in the user's community. Read-only. "
            + "Returns auction ID, season name, format, status, team count, and player count.")
    public List<Map<String, Object>> listMyCommunityAuctions() {
        UserContext ctx = AgentSecurityContext.get();
        log.debug("listMyCommunityAuctions: community={}", ctx.communityId());

        return em.createQuery(
                        "SELECT c.id, c.seasonName, c.auctionFormat, c.status, c.totalTeams, c.totalPlayers " +
                        "FROM AuctionConfig c WHERE c.createdBy.community.id = :comId " +
                        "ORDER BY c.id DESC", Object[].class)
                .setParameter("comId", ctx.communityId())
                .getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r[0]);
                    m.put("season_name", r[1]);
                    m.put("format", r[2] != null ? r[2].toString() : null);
                    m.put("status", r[3] != null ? r[3].toString() : null);
                    m.put("total_teams", r[4]);
                    m.put("total_players", r[5]);
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "Get auction configuration, status, and progress breakdown. "
            + "Community-scoped, read-only.")
    public Object getAuctionStatus(
            @ToolParam(description = "Auction config ID") Long auctionConfigId) {

        if (!isAuctionInCommunity(auctionConfigId)) {
            return accessDenied("auction not in your community");
        }

        var rows = em.createQuery(
                        "SELECT c.seasonName, c.auctionFormat, c.totalTeams, c.totalPlayers, " +
                        "c.budgetPerTeam, c.basePrice, c.bidIncrementDefault, c.bidTimerSeconds, " +
                        "c.rtmEnabled, c.unsoldRule, c.status " +
                        "FROM AuctionConfig c WHERE c.id = :cid", Object[].class)
                .setParameter("cid", auctionConfigId)
                .getResultList();

        if (rows.isEmpty()) return Map.of("error", "Auction not found");

        Object[] c = rows.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("season_name", c[0]);
        result.put("format", c[1] != null ? c[1].toString() : null);
        result.put("total_teams", c[2]);
        result.put("total_players", c[3]);
        result.put("budget_per_team", c[4]);
        result.put("base_price", c[5]);
        result.put("bid_increment", c[6]);
        result.put("bid_timer_seconds", c[7]);
        result.put("rtm_enabled", c[8]);
        result.put("unsold_rule", c[9] != null ? c[9].toString() : null);
        result.put("status", c[10] != null ? c[10].toString() : null);

        // Player status breakdown
        var statusCounts = em.createQuery(
                        "SELECT p.status, COUNT(p) FROM AuctionPlayer p " +
                        "WHERE p.config.id = :cid GROUP BY p.status", Object[].class)
                .setParameter("cid", auctionConfigId)
                .getResultList();

        Map<String, Long> progress = new LinkedHashMap<>();
        statusCounts.forEach(s -> progress.put(
                s[0].toString().toLowerCase(),
                ((Number) s[1]).longValue()));
        result.put("progress", progress);

        return result;
    }

    @Tool(description = "Search auction players by category, status, name, or max base price. "
            + "Community-scoped, read-only. Returns up to 10 results by default.")
    public Object searchPlayers(
            @ToolParam(description = "Auction config ID") Long auctionConfigId,
            @ToolParam(required = false, description = "Category: BATSMEN, BOWLERS, ALL_ROUNDERS, WICKET_KEEPERS")
            String category,
            @ToolParam(required = false, description = "Status: QUEUED, SELLING, SOLD, PASSED, RETAINED")
            String status,
            @ToolParam(required = false, description = "Player name (partial match)")
            String playerName,
            @ToolParam(required = false, description = "Maximum base price filter")
            Long maxBasePrice,
            @ToolParam(required = false, description = "Max results (default 10)")
            Integer limit) {

        if (!isAuctionInCommunity(auctionConfigId)) {
            return accessDenied("auction not in your community");
        }

        StringBuilder jpql = new StringBuilder(
                "SELECT p.playerName, p.category, p.playerRole, p.age, p.basePrice, " +
                "p.status, p.statsJson, p.queueOrder, p.soldPrice, p.assignedTeam.teamName " +
                "FROM AuctionPlayer p WHERE p.config.id = :cid");

        if (category != null) jpql.append(" AND p.category = :cat");
        if (status != null) jpql.append(" AND p.status = :st");
        if (playerName != null) jpql.append(" AND LOWER(p.playerName) LIKE LOWER(:nm)");
        if (maxBasePrice != null) jpql.append(" AND p.basePrice <= :mx");
        jpql.append(" ORDER BY p.queueOrder ASC");

        var query = em.createQuery(jpql.toString(), Object[].class)
                .setParameter("cid", auctionConfigId);

        if (category != null) query.setParameter("cat", category);
        if (status != null) query.setParameter("st", status);
        if (playerName != null) query.setParameter("nm", "%" + playerName + "%");
        if (maxBasePrice != null) query.setParameter("mx", maxBasePrice.intValue());
        query.setMaxResults(limit != null ? limit : 10);

        return query.getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", r[0]);
                    m.put("category", r[1]);
                    m.put("role", r[2]);
                    m.put("age", r[3]);
                    m.put("base_price", r[4]);
                    m.put("status", r[5] != null ? r[5].toString() : null);
                    m.put("stats", parseStats((String) r[6]));
                    m.put("queue_order", r[7]);
                    m.put("sold_price", r[8]);
                    m.put("assigned_team", r[9]);
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "Check team budgets, spending, and utilization. "
            + "Community-scoped, read-only. Team owners can filter to their own team.")
    public Object checkTeamBudget(
            @ToolParam(description = "Auction config ID") Long auctionConfigId,
            @ToolParam(required = false, description = "Team name filter (partial match)")
            String teamName,
            @ToolParam(required = false, description = "Only show current user's team")
            Boolean myTeamOnly) {

        if (!isAuctionInCommunity(auctionConfigId)) {
            return accessDenied("auction not in your community");
        }
        UserContext ctx = AgentSecurityContext.get();

        StringBuilder jpql = new StringBuilder(
                "SELECT t.id, t.teamName, t.ownerName, t.ownerUser.id, t.colorHex, " +
                "t.totalBudget, t.remainingBudget, t.spent " +
                "FROM AuctionTeam t WHERE t.config.id = :cid");

        if (Boolean.TRUE.equals(myTeamOnly) && ctx.teamId() != null) {
            jpql.append(" AND t.id = :tid");
        }
        if (teamName != null) {
            jpql.append(" AND LOWER(t.teamName) LIKE LOWER(:tn)");
        }
        jpql.append(" ORDER BY t.teamName");

        var query = em.createQuery(jpql.toString(), Object[].class)
                .setParameter("cid", auctionConfigId);

        if (Boolean.TRUE.equals(myTeamOnly) && ctx.teamId() != null) {
            query.setParameter("tid", ctx.teamId());
        }
        if (teamName != null) {
            query.setParameter("tn", "%" + teamName + "%");
        }

        return query.getResultList().stream()
                .map(r -> {
                    long total = ((Number) r[5]).longValue();
                    long remaining = ((Number) r[6]).longValue();
                    long spent = ((Number) r[7]).longValue();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("team_name", r[1]);
                    m.put("owner_name", r[2]);
                    m.put("color", r[4]);
                    m.put("total_budget", total);
                    m.put("remaining_budget", remaining);
                    m.put("spent", spent);
                    m.put("utilization_pct", total > 0
                            ? Math.round((double) spent / total * 100) : 0);
                    m.put("is_my_team", ctx.userId().equals(
                            r[3] != null ? ((Number) r[3]).longValue() : null));
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "Compare 2-5 players side-by-side with stats. Community-scoped, read-only.")
    public Object comparePlayers(
            @ToolParam(description = "Auction config ID") Long auctionConfigId,
            @ToolParam(description = "Player names to compare (2-5 names)")
            List<String> playerNames) {

        if (!isAuctionInCommunity(auctionConfigId)) {
            return accessDenied("auction not in your community");
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (String name : playerNames) {
            var rows = em.createQuery(
                            "SELECT p.playerName, p.category, p.playerRole, p.age, p.basePrice, " +
                            "p.status, p.statsJson, p.soldPrice, p.assignedTeam.teamName " +
                            "FROM AuctionPlayer p WHERE p.config.id = :cid " +
                            "AND LOWER(p.playerName) LIKE LOWER(:nm)", Object[].class)
                    .setParameter("cid", auctionConfigId)
                    .setParameter("nm", "%" + name + "%")
                    .setMaxResults(1)
                    .getResultList();

            if (rows.isEmpty()) {
                results.add(Map.of("search_name", name, "error", "Player not found"));
                continue;
            }
            Object[] r = rows.get(0);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", r[0]);
            m.put("category", r[1]);
            m.put("role", r[2]);
            m.put("age", r[3]);
            m.put("base_price", r[4]);
            m.put("status", r[5] != null ? r[5].toString() : null);
            m.put("stats", parseStats((String) r[6]));
            m.put("sold_price", r[7]);
            m.put("assigned_team", r[8]);
            results.add(m);
        }
        return results;
    }

    @Tool(description = "Get the upcoming player queue (next players up for bidding). "
            + "Community-scoped, read-only.")
    public Object getPlayerQueue(
            @ToolParam(description = "Auction config ID") Long auctionConfigId,
            @ToolParam(required = false, description = "Max results (default 10)") Integer limit) {

        if (!isAuctionInCommunity(auctionConfigId)) {
            return accessDenied("auction not in your community");
        }

        return em.createQuery(
                        "SELECT p.queueOrder, p.playerName, p.category, p.playerRole, p.basePrice " +
                        "FROM AuctionPlayer p WHERE p.config.id = :cid AND p.status = 'QUEUED' " +
                        "ORDER BY p.queueOrder ASC", Object[].class)
                .setParameter("cid", auctionConfigId)
                .setMaxResults(limit != null ? limit : 10)
                .getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("queue_position", r[0]);
                    m.put("player_name", r[1]);
                    m.put("category", r[2]);
                    m.put("role", r[3]);
                    m.put("base_price", r[4]);
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ── ADMIN-ONLY WRITE TOOLS ─────────────────────────────────────────

    @Tool(description = "[SUPER_ADMIN ONLY] Update auction status. "
            + "Valid transitions: DRAFT→ACTIVE, ACTIVE→LIVE, LIVE→COMPLETED. "
            + "Requires explicit confirmation.")
    @Transactional
    public Object updateAuctionStatus(
            @ToolParam(description = "Auction config ID") Long auctionConfigId,
            @ToolParam(description = "New status: DRAFT, ACTIVE, LIVE, COMPLETED, CANCELLED")
            String newStatus,
            @ToolParam(description = "Has the user explicitly confirmed this change?")
            Boolean confirmed) {

        UserContext ctx = AgentSecurityContext.get();
        if (!ctx.isSuperAdmin()) return writeNotAllowed();
        if (!isAuctionInCommunity(auctionConfigId)) {
            return accessDenied("auction not in your community");
        }
        if (!Boolean.TRUE.equals(confirmed)) {
            return Map.of("requires_confirmation", true,
                    "message", "Please confirm: change auction status to " + newStatus + "?");
        }

        int updated = em.createQuery(
                        "UPDATE AuctionConfig c SET c.status = :st, c.updatedAt = CURRENT_TIMESTAMP " +
                        "WHERE c.id = :cid AND c.createdBy.community.id = :comId")
                .setParameter("st", com.manacommunity.api.model.AuctionConfig.AuctionStatus.valueOf(newStatus))
                .setParameter("cid", auctionConfigId)
                .setParameter("comId", ctx.communityId())
                .executeUpdate();

        log.info("Auction {} status updated to {} by admin user={}", auctionConfigId, newStatus, ctx.userId());
        return Map.of("success", updated > 0, "new_status", newStatus);
    }

    @Tool(description = "[SUPER_ADMIN ONLY] Reset PASSED players back to QUEUED. "
            + "Does NOT delete any data. Requires confirmation.")
    @Transactional
    public Object resetPassedPlayers(
            @ToolParam(description = "Auction config ID") Long auctionConfigId,
            @ToolParam(description = "Has the user explicitly confirmed this action?")
            Boolean confirmed) {

        UserContext ctx = AgentSecurityContext.get();
        if (!ctx.isSuperAdmin()) return writeNotAllowed();
        if (!isAuctionInCommunity(auctionConfigId)) {
            return accessDenied("auction not in your community");
        }

        Long passedCount = em.createQuery(
                        "SELECT COUNT(p) FROM AuctionPlayer p " +
                        "WHERE p.config.id = :cid AND p.status = 'PASSED'", Long.class)
                .setParameter("cid", auctionConfigId)
                .getSingleResult();

        if (!Boolean.TRUE.equals(confirmed)) {
            return Map.of("requires_confirmation", true,
                    "passed_count", passedCount,
                    "message", "Move " + passedCount + " passed players back to queue?");
        }

        int updated = em.createQuery(
                        "UPDATE AuctionPlayer p SET p.status = 'QUEUED' " +
                        "WHERE p.config.id = :cid AND p.status = 'PASSED'")
                .setParameter("cid", auctionConfigId)
                .executeUpdate();

        log.info("Reset {} passed players to QUEUED for auction {} by admin user={}",
                updated, auctionConfigId, ctx.userId());
        return Map.of("success", true, "players_reset", updated);
    }

    // ── Utility ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseStats(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of("raw", json);
        }
    }
}
