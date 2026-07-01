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
 * AI tools for querying the audit trail — admin-only, for investigating
 * who changed what and when.
 *
 * <p>Reads from {@code AuditLog} which tracks every sensitive mutation with
 * actor, action, module, entity, old/new values, IP, and timestamp.</p>
 */
@Slf4j
@Component
@Transactional(readOnly = true)
public class AdminAuditTools {

    @PersistenceContext
    private EntityManager em;

    @Tool(description = "[ADMIN ONLY] Search the audit trail — find who performed actions, "
            + "what changed, and when. Can filter by module (AUCTION, TOURNAMENT, EVENT, USER, "
            + "BILLING), action (CREATE, UPDATE, DELETE), user, or time range. Read-only.")
    public Object searchAuditLog(
            @ToolParam(required = false, description = "Module filter: AUCTION, TOURNAMENT, EVENT, USER, BILLING, etc.")
            String module,
            @ToolParam(required = false, description = "Action filter: CREATE, UPDATE, DELETE, LOGIN, APPROVE, REJECT, etc.")
            String action,
            @ToolParam(required = false, description = "User name filter (partial match)") String userName,
            @ToolParam(required = false, description = "Entity name filter (e.g. AuctionConfig, TournamentMatch)")
            String entityName,
            @ToolParam(required = false, description = "Max results (default 20)") Integer limit) {

        UserContext ctx = AgentSecurityContext.get();
        if (!ctx.isAdmin()) {
            return Map.of("error", "Admin access required to view audit logs.");
        }

        StringBuilder jpql = new StringBuilder(
                "SELECT a.id, a.action, a.module, a.entityName, a.entityId, " +
                "a.oldValue, a.newValue, a.ipAddress, a.createdAt, u.fullName " +
                "FROM AuditLog a LEFT JOIN AppUser u ON a.userId = u.id " +
                "WHERE 1=1");

        if (module != null) jpql.append(" AND UPPER(a.module) = :mod");
        if (action != null) jpql.append(" AND UPPER(a.action) = :act");
        if (userName != null) jpql.append(" AND LOWER(u.fullName) LIKE LOWER(:un)");
        if (entityName != null) jpql.append(" AND LOWER(a.entityName) LIKE LOWER(:en)");
        jpql.append(" ORDER BY a.createdAt DESC");

        var query = em.createQuery(jpql.toString(), Object[].class);
        if (module != null) query.setParameter("mod", module.toUpperCase());
        if (action != null) query.setParameter("act", action.toUpperCase());
        if (userName != null) query.setParameter("un", "%" + userName + "%");
        if (entityName != null) query.setParameter("en", "%" + entityName + "%");
        query.setMaxResults(limit != null ? limit : 20);

        return query.getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("log_id", r[0]);
                    m.put("action", r[1]);
                    m.put("module", r[2]);
                    m.put("entity", r[3]);
                    m.put("entity_id", r[4]);
                    m.put("old_value", truncate((String) r[5], 200));
                    m.put("new_value", truncate((String) r[6], 200));
                    m.put("ip_address", r[7]);
                    m.put("timestamp", r[8] != null ? r[8].toString() : null);
                    m.put("performed_by", r[9]);
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "[ADMIN ONLY] Get an audit summary — count of actions by module and "
            + "action type over a recent period. Shows which areas have the most activity.")
    public Object getAuditSummary() {
        UserContext ctx = AgentSecurityContext.get();
        if (!ctx.isAdmin()) {
            return Map.of("error", "Admin access required.");
        }

        Map<String, Object> result = new LinkedHashMap<>();

        // By module
        var byModule = em.createQuery(
                "SELECT a.module, COUNT(a) FROM AuditLog a " +
                "GROUP BY a.module ORDER BY COUNT(a) DESC", Object[].class)
                .getResultList();
        result.put("by_module", byModule.stream()
                .map(r -> Map.of("module", r[0], "count", r[1]))
                .collect(Collectors.toList()));

        // By action
        var byAction = em.createQuery(
                "SELECT a.action, COUNT(a) FROM AuditLog a " +
                "GROUP BY a.action ORDER BY COUNT(a) DESC", Object[].class)
                .getResultList();
        result.put("by_action", byAction.stream()
                .map(r -> Map.of("action", r[0], "count", r[1]))
                .collect(Collectors.toList()));

        // Most active users
        var topUsers = em.createQuery(
                "SELECT u.fullName, COUNT(a) FROM AuditLog a " +
                "JOIN AppUser u ON a.userId = u.id " +
                "GROUP BY u.fullName ORDER BY COUNT(a) DESC", Object[].class)
                .setMaxResults(10)
                .getResultList();
        result.put("most_active_users", topUsers.stream()
                .map(r -> Map.of("user", r[0], "actions", r[1]))
                .collect(Collectors.toList()));

        // Total entries
        Long total = em.createQuery("SELECT COUNT(a) FROM AuditLog a", Long.class).getSingleResult();
        result.put("total_entries", total);

        return result;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
