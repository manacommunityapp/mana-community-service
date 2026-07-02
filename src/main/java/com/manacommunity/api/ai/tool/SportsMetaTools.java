package com.manacommunity.api.ai.tool;

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
 * AI tools for the sports catalogue: available sports, player categories (age/gender
 * divisions), and format metadata.
 *
 * <p>Read tools are scoped to active entries visible to the user's community.
 * Admin write tools (create/update sport, create/update category) require SUPER_ADMIN
 * or ADMIN role and include confirmation gates.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
@Transactional(readOnly = true)
public class SportsMetaTools {

    @PersistenceContext
    private EntityManager em;

    // ── READ ────────────────────────────────────────────────────────────

    @Tool(description = "List all available sports in the platform with their formats "
            + "(e.g. SINGLES, DOUBLES, TEAM). Shows global sports plus community-specific ones.")
    public List<Map<String, Object>> listAvailableSports() {
        UserContext ctx = AgentSecurityContext.get();

        return em.createQuery(
                        "SELECT s.id, s.name, s.icon, s.iconUrl, s.formats, s.communityId " +
                        "FROM SportsMeta s WHERE s.active = true " +
                        "AND (s.communityId IS NULL OR s.communityId = :comId) " +
                        "ORDER BY s.name", Object[].class)
                .setParameter("comId", ctx.communityId())
                .getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("sport_id", r[0]);
                    m.put("name", r[1]);
                    m.put("icon", r[2]);
                    m.put("icon_url", r[3]);
                    m.put("formats", r[4]);
                    m.put("scope", r[5] == null ? "GLOBAL" : "COMMUNITY");
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "List player categories (age groups and gender divisions) available "
            + "for event registration. Shows DEFAULT categories plus community-specific ones. "
            + "Each category defines min/max age, gender, and type (MENS, WOMENS, BOYS, GIRLS, etc.).")
    public List<Map<String, Object>> listPlayerCategories(
            @ToolParam(required = false, description = "Filter by gender: MALE, FEMALE, ALL")
            String gender,
            @ToolParam(required = false, description = "Filter by category type: MENS, WOMENS, BOYS, GIRLS, KIDS, SENIORS")
            String categoryType) {

        UserContext ctx = AgentSecurityContext.get();

        StringBuilder jpql = new StringBuilder(
                "SELECT c.id, c.name, c.category_type, c.description, c.minAge, c.maxAge, " +
                "c.gender, c.type " +
                "FROM PlayerCategory c WHERE (c.type = 'DEFAULT' " +
                "OR c.community.id = :comId)");

        if (gender != null) jpql.append(" AND c.gender = :gen");
        if (categoryType != null) jpql.append(" AND c.category_type = :catType");
        jpql.append(" ORDER BY c.minAge, c.name");

        var query = em.createQuery(jpql.toString(), Object[].class)
                .setParameter("comId", ctx.communityId());

        if (gender != null) query.setParameter("gen", gender);
        if (categoryType != null) query.setParameter("catType", categoryType);

        return query.getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("category_id", r[0]);
                    m.put("name", r[1]);
                    m.put("category_type", r[2]);
                    m.put("description", r[3]);
                    m.put("min_age", r[4]);
                    m.put("max_age", r[5]);
                    m.put("gender", r[6]);
                    m.put("scope", "DEFAULT".equals(r[7]) ? "GLOBAL" : "COMMUNITY");
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "Get detailed information about a specific sport — name, icon, "
            + "available formats, and how many active events use it in the community.")
    public Object getSportDetails(
            @ToolParam(description = "Sport ID") Long sportId) {

        UserContext ctx = AgentSecurityContext.get();

        var rows = em.createQuery(
                        "SELECT s.id, s.name, s.icon, s.iconUrl, s.formats, s.communityId " +
                        "FROM SportsMeta s WHERE s.id = :sid AND s.active = true " +
                        "AND (s.communityId IS NULL OR s.communityId = :comId)", Object[].class)
                .setParameter("sid", sportId)
                .setParameter("comId", ctx.communityId())
                .getResultList();

        if (rows.isEmpty()) return Map.of("error", "Sport not found or not accessible");

        Object[] r = rows.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sport_id", r[0]);
        result.put("name", r[1]);
        result.put("icon", r[2]);
        result.put("icon_url", r[3]);
        result.put("formats", r[4]);
        result.put("scope", r[5] == null ? "GLOBAL" : "COMMUNITY");

        // Count active events using this sport in user's community
        Long eventCount = em.createQuery(
                        "SELECT COUNT(e) FROM SportsEvent e " +
                        "WHERE e.sport.id = :sid AND e.community.id = :comId AND e.active = true",
                        Long.class)
                .setParameter("sid", sportId)
                .setParameter("comId", ctx.communityId())
                .getSingleResult();
        result.put("active_events_in_community", eventCount);

        // Count total registrations across events for this sport
        Long regCount = em.createQuery(
                        "SELECT COUNT(r) FROM SportsEventRegistration r " +
                        "WHERE r.event.sport.id = :sid AND r.event.community.id = :comId",
                        Long.class)
                .setParameter("sid", sportId)
                .setParameter("comId", ctx.communityId())
                .getSingleResult();
        result.put("total_registrations", regCount);

        return result;
    }

    // ── ADMIN WRITE ────────────────────────────────────────────────────

    @Tool(description = "[ADMIN ONLY] Create a new sport in the community's catalogue. "
            + "Requires confirmation. Does not delete anything.")
    @Transactional
    public Object createSport(
            @ToolParam(description = "Sport name (e.g. Badminton, Cricket, Football)") String name,
            @ToolParam(required = false, description = "Icon emoji (e.g. 🏏)") String icon,
            @ToolParam(required = false, description = "Formats: SINGLES, DOUBLES, MIXED_DOUBLES, TEAM (comma-separated)")
            String formats,
            @ToolParam(description = "Has the user explicitly confirmed?") Boolean confirmed) {

        UserContext ctx = AgentSecurityContext.get();
        if (!ctx.isAdmin()) {
            return Map.of("error", true, "message", "Only admins can create sports.");
        }
        if (!Boolean.TRUE.equals(confirmed)) {
            return Map.of("requires_confirmation", true,
                    "message", "Create sport '" + name + "' in your community?");
        }

        // Check for duplicates
        Long exists = em.createQuery(
                        "SELECT COUNT(s) FROM SportsMeta s WHERE LOWER(s.name) = LOWER(:nm) " +
                        "AND s.active = true AND (s.communityId IS NULL OR s.communityId = :comId)",
                        Long.class)
                .setParameter("nm", name)
                .setParameter("comId", ctx.communityId())
                .getSingleResult();

        if (exists > 0) {
            return Map.of("error", true, "message", "A sport named '" + name + "' already exists.");
        }

        em.createNativeQuery(
                        "INSERT INTO sports_meta (name, icon, format, community_id, active, created_at, updated_at) " +
                        "VALUES (:nm, :ico, :fmt, :comId, true, NOW(), NOW())")
                .setParameter("nm", name)
                .setParameter("ico", icon)
                .setParameter("fmt", formats != null ? formats : "")
                .setParameter("comId", ctx.communityId())
                .executeUpdate();

        log.info("Sport '{}' created by admin user={} in community={}", name, ctx.userId(), ctx.communityId());
        return Map.of("success", true, "sport_name", name);
    }
}
