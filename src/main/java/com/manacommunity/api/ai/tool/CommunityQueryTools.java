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
 * Spring AI tool methods for community-level queries: sports events, user
 * registrations, venues, and courts.
 *
 * <p>All queries are scoped to the user's community.</p>
 */
@Slf4j
@Component
@Transactional(readOnly = true)
public class CommunityQueryTools {

    @PersistenceContext
    private EntityManager em;

    @Tool(description = "List sports events in the user's community. "
            + "Can filter by status (open/closed/all). Community-scoped, read-only.")
    public List<Map<String, Object>> listSportsEvents(
            @ToolParam(required = false, description = "Filter: OPEN, CLOSED, or ALL (default ALL)")
            String filter) {

        UserContext ctx = AgentSecurityContext.get();
        log.debug("listSportsEvents: community={}, filter={}", ctx.communityId(), filter);

        StringBuilder jpql = new StringBuilder(
                "SELECT e.id, e.name, e.eventDateStart, e.eventDateEnd, " +
                "s.name, v.name, e.maxParticipants, t.registrationStatus " +
                "FROM SportsEvent e " +
                "LEFT JOIN e.sport s LEFT JOIN e.venue v " +
                "LEFT JOIN e.tournament t " +
                "WHERE e.community.id = :comId");

        if ("OPEN".equalsIgnoreCase(filter)) {
            jpql.append(" AND t.registrationStatus IN ('REGISTRATION_OPEN', 'LIVE')");
        } else if ("CLOSED".equalsIgnoreCase(filter)) {
            jpql.append(" AND (t.registrationStatus = 'REGISTRATION_CLOSED' OR t.registrationStatus = 'COMPLETED')");
        }
        jpql.append(" ORDER BY e.eventDateStart DESC");

        return em.createQuery(jpql.toString(), Object[].class)
                .setParameter("comId", ctx.communityId())
                .setMaxResults(20)
                .getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("event_id", r[0]);
                    m.put("name", r[1]);
                    m.put("start_date", r[2] != null ? r[2].toString() : null);
                    m.put("end_date", r[3] != null ? r[3].toString() : null);
                    m.put("sport", r[4]);
                    m.put("venue", r[5]);
                    m.put("max_participants", r[6]);
                    m.put("registration_status", r[7] != null ? r[7].toString() : null);
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "Check the current user's event registrations — what events they've signed up for, "
            + "their registration status, and category. Read-only.")
    public List<Map<String, Object>> getMyRegistrations() {
        UserContext ctx = AgentSecurityContext.get();

        return em.createQuery(
                        "SELECT r.id, e.id, e.name, e.eventDateStart, s.name, " +
                        "r.status, r.matchType, pc.name " +
                        "FROM SportsEventRegistration r " +
                        "JOIN r.event e LEFT JOIN e.sport s LEFT JOIN r.category pc " +
                        "WHERE r.user.id = :uid AND e.community.id = :comId " +
                        "ORDER BY e.eventDateStart DESC", Object[].class)
                .setParameter("uid", ctx.userId())
                .setParameter("comId", ctx.communityId())
                .getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("registration_id", r[0]);
                    m.put("event_id", r[1]);
                    m.put("event_name", r[2]);
                    m.put("event_date", r[3] != null ? r[3].toString() : null);
                    m.put("sport", r[4]);
                    m.put("status", r[5] != null ? r[5].toString() : null);
                    m.put("match_type", r[6] != null ? r[6].toString() : null);
                    m.put("category", r[7]);
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "List venues and their courts in the user's community. "
            + "Shows venue name, address, and available courts. Read-only.")
    public List<Map<String, Object>> listVenues() {
        UserContext ctx = AgentSecurityContext.get();

        var venues = em.createQuery(
                        "SELECT v.id, v.name, v.address, v.city, v.mapsUrl " +
                        "FROM Venue v WHERE v.community.id = :comId " +
                        "ORDER BY v.name", Object[].class)
                .setParameter("comId", ctx.communityId())
                .getResultList();

        List<Map<String, Object>> results = new ArrayList<>();
        for (Object[] v : venues) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("venue_id", v[0]);
            m.put("name", v[1]);
            m.put("address", v[2]);
            m.put("city", v[3]);
            m.put("maps_url", v[4]);

            // Fetch courts for this venue
            var courts = em.createQuery(
                            "SELECT c.id, c.name, c.courtType, c.surface " +
                            "FROM Court c WHERE c.venue.id = :vid ORDER BY c.name", Object[].class)
                    .setParameter("vid", v[0])
                    .getResultList();

            m.put("courts", courts.stream()
                    .map(c -> {
                        Map<String, Object> cm = new LinkedHashMap<>();
                        cm.put("court_id", c[0]);
                        cm.put("name", c[1]);
                        cm.put("type", c[2]);
                        cm.put("surface", c[3]);
                        return cm;
                    })
                    .collect(Collectors.toList()));

            results.add(m);
        }
        return results;
    }

    @Tool(description = "Get a summary of the user's community — member count, "
            + "active events, and recent activity. Read-only.")
    public Map<String, Object> getCommunityOverview() {
        UserContext ctx = AgentSecurityContext.get();

        // Community info
        var comRows = em.createQuery(
                        "SELECT c.name, c.type, c.city, c.state " +
                        "FROM Community c WHERE c.id = :comId", Object[].class)
                .setParameter("comId", ctx.communityId())
                .getResultList();

        if (comRows.isEmpty()) return Map.of("error", "Community not found");

        Object[] com = comRows.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("community_name", com[0]);
        result.put("type", com[1]);
        result.put("city", com[2]);
        result.put("state", com[3]);

        // Active member count
        Long memberCount = em.createQuery(
                        "SELECT COUNT(u) FROM AppUser u " +
                        "WHERE u.community.id = :comId AND u.isActive = true", Long.class)
                .setParameter("comId", ctx.communityId())
                .getSingleResult();
        result.put("active_members", memberCount);

        // Open event count
        Long openEvents = em.createQuery(
                        "SELECT COUNT(e) FROM SportsEvent e LEFT JOIN e.tournament t " +
                        "WHERE e.community.id = :comId " +
                        "AND (t IS NULL OR t.registrationStatus IN ('REGISTRATION_OPEN', 'LIVE'))",
                        Long.class)
                .setParameter("comId", ctx.communityId())
                .getSingleResult();
        result.put("open_events", openEvents);

        // Total venues
        Long venueCount = em.createQuery(
                        "SELECT COUNT(v) FROM Venue v WHERE v.community.id = :comId", Long.class)
                .setParameter("comId", ctx.communityId())
                .getSingleResult();
        result.put("venues", venueCount);

        return result;
    }
}
