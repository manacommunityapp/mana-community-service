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
 * AI tools for sports event queries and management.
 *
 * <p>Covers detailed event views, registration analytics, participant lists,
 * sponsor info, and admin-level registration management (confirm/reject).</p>
 *
 * <p>All queries scoped to the user's community via {@code e.community.id}.</p>
 */
@Slf4j
@Component
@Transactional(readOnly = true)
public class SportsEventTools {

    @PersistenceContext
    private EntityManager em;

    // ── Community scoping helper ───────────────────────────────────────

    private boolean isEventInCommunity(Long eventId) {
        if (eventId == null) return false;
        UserContext ctx = AgentSecurityContext.get();
        return em.createQuery(
                        "SELECT COUNT(e) FROM SportsEvent e " +
                        "WHERE e.id = :eid AND e.community.id = :comId", Long.class)
                .setParameter("eid", eventId)
                .setParameter("comId", ctx.communityId())
                .getSingleResult() > 0;
    }

    // ── READ ────────────────────────────────────────────────────────────

    @Tool(description = "Get full details of a sports event — sport, venue, dates, age limits, "
            + "registration dates, categories, sponsors, format, tournament type, contact info, "
            + "and registration counts. Community-scoped, read-only.")
    public Object getEventDetails(
            @ToolParam(description = "Event ID") Long eventId) {

        if (!isEventInCommunity(eventId)) {
            return Map.of("error", "Event not found in your community");
        }

        var rows = em.createQuery(
                "SELECT e.id, e.name, e.description, e.icon, " +
                "e.minAge, e.maxAge, e.minPlayers, e.maxPlayers, e.gender, " +
                "e.eventDateStart, e.eventDateEnd, " +
                "e.registrationDateStart, e.registrationDateEnd, " +
                "e.maxParticipants, e.startTime, e.dueTime, " +
                "e.format, e.tournamentType, e.auctionStatus, e.auctionEnabled, " +
                "e.adminApprovalRequired, e.active, " +
                "e.contactName, e.contactNumber, e.contactEmail, " +
                "e.bannerImage, e.tournamentLevel, " +
                "s.name, s.icon, v.name, v.address, v.city, " +
                "t.registrationStatus " +
                "FROM SportsEvent e " +
                "LEFT JOIN e.sport s LEFT JOIN e.venue v LEFT JOIN e.tournament t " +
                "WHERE e.id = :eid", Object[].class)
                .setParameter("eid", eventId)
                .getResultList();

        if (rows.isEmpty()) return Map.of("error", "Event not found");

        Object[] r = rows.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("event_id", r[0]);
        result.put("name", r[1]);
        result.put("description", r[2]);
        result.put("icon", r[3]);
        result.put("min_age", r[4]);
        result.put("max_age", r[5]);
        result.put("min_players", r[6]);
        result.put("max_players", r[7]);
        result.put("gender", r[8]);
        result.put("event_date_start", r[9] != null ? r[9].toString() : null);
        result.put("event_date_end", r[10] != null ? r[10].toString() : null);
        result.put("registration_date_start", r[11] != null ? r[11].toString() : null);
        result.put("registration_date_end", r[12] != null ? r[12].toString() : null);
        result.put("max_participants", r[13]);
        result.put("start_time", r[14]);
        result.put("due_time", r[15]);
        result.put("format", r[16]);
        result.put("tournament_type", r[17] != null ? r[17].toString() : null);
        result.put("auction_status", r[18] != null ? r[18].toString() : null);
        result.put("auction_enabled", r[19]);
        result.put("admin_approval_required", r[20]);
        result.put("active", r[21]);
        result.put("contact_name", r[22]);
        result.put("contact_number", r[23]);
        result.put("contact_email", r[24]);
        result.put("sport", r[27]);
        result.put("sport_icon", r[28]);
        result.put("venue", r[29]);
        result.put("venue_address", r[30]);
        result.put("venue_city", r[31]);
        result.put("registration_status", r[32] != null ? r[32].toString() : null);

        // Registration counts by status
        var statusCounts = em.createQuery(
                        "SELECT r.status, COUNT(r) FROM SportsEventRegistration r " +
                        "WHERE r.event.id = :eid GROUP BY r.status", Object[].class)
                .setParameter("eid", eventId)
                .getResultList();

        Map<String, Long> regBreakdown = new LinkedHashMap<>();
        long totalRegs = 0;
        for (Object[] sc : statusCounts) {
            String status = sc[0].toString().toLowerCase();
            long count = ((Number) sc[1]).longValue();
            regBreakdown.put(status, count);
            totalRegs += count;
        }
        result.put("total_registrations", totalRegs);
        result.put("registration_breakdown", regBreakdown);

        // Categories
        var categories = em.createQuery(
                        "SELECT c.name, c.category_type, c.minAge, c.maxAge, c.gender " +
                        "FROM SportsEvent e JOIN e.categories c WHERE e.id = :eid", Object[].class)
                .setParameter("eid", eventId)
                .getResultList();

        result.put("categories", categories.stream()
                .map(c -> {
                    Map<String, Object> cm = new LinkedHashMap<>();
                    cm.put("name", c[0]);
                    cm.put("type", c[1]);
                    cm.put("min_age", c[2]);
                    cm.put("max_age", c[3]);
                    cm.put("gender", c[4]);
                    return cm;
                })
                .collect(Collectors.toList()));

        // Sponsors
        var sponsors = em.createQuery(
                        "SELECT sp.name, sp.category, sp.url FROM SportsEventSponsor sp " +
                        "WHERE sp.event.id = :eid ORDER BY sp.category", Object[].class)
                .setParameter("eid", eventId)
                .getResultList();

        result.put("sponsors", sponsors.stream()
                .map(sp -> Map.of("name", sp[0], "category", sp[1],
                        "url", sp[2] != null ? sp[2] : ""))
                .collect(Collectors.toList()));

        return result;
    }

    @Tool(description = "Get registration analytics for an event — breakdown by status, category, "
            + "gender, and age range. Community-scoped, read-only.")
    public Object getEventRegistrationAnalytics(
            @ToolParam(description = "Event ID") Long eventId) {

        if (!isEventInCommunity(eventId)) {
            return Map.of("error", "Event not found in your community");
        }

        Map<String, Object> result = new LinkedHashMap<>();

        // Total and breakdown by status
        var byStatus = em.createQuery(
                        "SELECT r.status, COUNT(r) FROM SportsEventRegistration r " +
                        "WHERE r.event.id = :eid GROUP BY r.status", Object[].class)
                .setParameter("eid", eventId)
                .getResultList();

        Map<String, Long> statusMap = new LinkedHashMap<>();
        byStatus.forEach(s -> statusMap.put(s[0].toString(), ((Number) s[1]).longValue()));
        result.put("by_status", statusMap);

        // By category
        var byCat = em.createQuery(
                        "SELECT COALESCE(c.name, 'Uncategorized'), COUNT(r) " +
                        "FROM SportsEventRegistration r LEFT JOIN r.category c " +
                        "WHERE r.event.id = :eid GROUP BY c.name", Object[].class)
                .setParameter("eid", eventId)
                .getResultList();

        Map<String, Long> catMap = new LinkedHashMap<>();
        byCat.forEach(c -> catMap.put((String) c[0], ((Number) c[1]).longValue()));
        result.put("by_category", catMap);

        // By match type
        var byMatch = em.createQuery(
                        "SELECT COALESCE(CAST(r.matchType AS string), 'UNSET'), COUNT(r) " +
                        "FROM SportsEventRegistration r " +
                        "WHERE r.event.id = :eid GROUP BY r.matchType", Object[].class)
                .setParameter("eid", eventId)
                .getResultList();

        Map<String, Long> matchMap = new LinkedHashMap<>();
        byMatch.forEach(mt -> matchMap.put((String) mt[0], ((Number) mt[1]).longValue()));
        result.put("by_match_type", matchMap);

        // Captain nominations
        Long captainNoms = em.createQuery(
                        "SELECT COUNT(r) FROM SportsEventRegistration r " +
                        "WHERE r.event.id = :eid AND r.captainNomination = true", Long.class)
                .setParameter("eid", eventId)
                .getSingleResult();
        Long captainConfirmed = em.createQuery(
                        "SELECT COUNT(r) FROM SportsEventRegistration r " +
                        "WHERE r.event.id = :eid AND r.captainConfirmation = true", Long.class)
                .setParameter("eid", eventId)
                .getSingleResult();
        result.put("captain_nominations", captainNoms);
        result.put("captain_confirmations", captainConfirmed);

        return result;
    }

    @Tool(description = "List registered participants for an event — names, status, category, "
            + "match type, and captain info. Community-scoped, read-only.")
    public Object listEventRegistrations(
            @ToolParam(description = "Event ID") Long eventId,
            @ToolParam(required = false, description = "Filter by status: PENDING, REGISTERED, CONFIRMED, WITHDRAWN, REJECTED")
            String status,
            @ToolParam(required = false, description = "Max results (default 20)") Integer limit) {

        if (!isEventInCommunity(eventId)) {
            return Map.of("error", "Event not found in your community");
        }

        StringBuilder jpql = new StringBuilder(
                "SELECT r.id, COALESCE(r.playerName, u.fullName), u.email, u.phone, " +
                "r.status, c.name, r.matchType, r.age, r.flatNumber, " +
                "r.captainNomination, r.captainConfirmation, r.proposedTeamName, " +
                "r.registeredAt " +
                "FROM SportsEventRegistration r " +
                "LEFT JOIN r.user u LEFT JOIN r.category c " +
                "WHERE r.event.id = :eid");

        if (status != null) jpql.append(" AND r.status = :st");
        jpql.append(" ORDER BY r.registeredAt ASC");

        var query = em.createQuery(jpql.toString(), Object[].class)
                .setParameter("eid", eventId);
        if (status != null) query.setParameter("st",
                com.manacommunity.api.model.SportsEventRegistration.RegistrationStatus.valueOf(status));
        query.setMaxResults(limit != null ? limit : 20);

        return query.getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("registration_id", r[0]);
                    m.put("player_name", r[1]);
                    m.put("email", r[2]);
                    m.put("phone", r[3]);
                    m.put("status", r[4] != null ? r[4].toString() : null);
                    m.put("category", r[5]);
                    m.put("match_type", r[6] != null ? r[6].toString() : null);
                    m.put("age", r[7]);
                    m.put("flat_number", r[8]);
                    m.put("captain_nominated", r[9]);
                    m.put("captain_confirmed", r[10]);
                    m.put("proposed_team", r[11]);
                    m.put("registered_at", r[12] != null ? r[12].toString() : null);
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "Search events across the community by sport name, event name, "
            + "or date range. Community-scoped, read-only.")
    public Object searchEvents(
            @ToolParam(required = false, description = "Sport name filter (partial match)")
            String sportName,
            @ToolParam(required = false, description = "Event name filter (partial match)")
            String eventName,
            @ToolParam(required = false, description = "Only show active events") Boolean activeOnly,
            @ToolParam(required = false, description = "Max results (default 15)") Integer limit) {

        UserContext ctx = AgentSecurityContext.get();

        StringBuilder jpql = new StringBuilder(
                "SELECT e.id, e.name, s.name, v.name, e.eventDateStart, e.eventDateEnd, " +
                "e.format, e.tournamentType, t.registrationStatus, e.maxParticipants " +
                "FROM SportsEvent e " +
                "LEFT JOIN e.sport s LEFT JOIN e.venue v LEFT JOIN e.tournament t " +
                "WHERE e.community.id = :comId");

        if (sportName != null) jpql.append(" AND LOWER(s.name) LIKE LOWER(:sn)");
        if (eventName != null) jpql.append(" AND LOWER(e.name) LIKE LOWER(:en)");
        if (Boolean.TRUE.equals(activeOnly)) jpql.append(" AND e.active = true");
        jpql.append(" ORDER BY e.eventDateStart DESC");

        var query = em.createQuery(jpql.toString(), Object[].class)
                .setParameter("comId", ctx.communityId());
        if (sportName != null) query.setParameter("sn", "%" + sportName + "%");
        if (eventName != null) query.setParameter("en", "%" + eventName + "%");
        query.setMaxResults(limit != null ? limit : 15);

        return query.getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("event_id", r[0]);
                    m.put("event_name", r[1]);
                    m.put("sport", r[2]);
                    m.put("venue", r[3]);
                    m.put("start_date", r[4] != null ? r[4].toString() : null);
                    m.put("end_date", r[5] != null ? r[5].toString() : null);
                    m.put("format", r[6]);
                    m.put("tournament_type", r[7] != null ? r[7].toString() : null);
                    m.put("registration_status", r[8] != null ? r[8].toString() : null);
                    m.put("max_participants", r[9]);
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ── ADMIN WRITE ────────────────────────────────────────────────────

    @Tool(description = "[ADMIN ONLY] Confirm a pending registration. Changes status from "
            + "PENDING to CONFIRMED. Requires confirmation.")
    @Transactional
    public Object confirmRegistration(
            @ToolParam(description = "Registration ID") Long registrationId,
            @ToolParam(description = "Has the user explicitly confirmed?") Boolean confirmed) {

        UserContext ctx = AgentSecurityContext.get();
        if (!ctx.isAdmin()) {
            return Map.of("error", true, "message", "Only admins can confirm registrations.");
        }

        // Verify registration belongs to community
        var rows = em.createQuery(
                        "SELECT r.id, COALESCE(r.playerName, u.fullName), r.status, e.name " +
                        "FROM SportsEventRegistration r JOIN r.event e LEFT JOIN r.user u " +
                        "WHERE r.id = :rid AND e.community.id = :comId", Object[].class)
                .setParameter("rid", registrationId)
                .setParameter("comId", ctx.communityId())
                .getResultList();

        if (rows.isEmpty()) return Map.of("error", "Registration not found in your community");

        Object[] r = rows.get(0);
        String currentStatus = r[2] != null ? r[2].toString() : "UNKNOWN";

        if (!Boolean.TRUE.equals(confirmed)) {
            return Map.of("requires_confirmation", true,
                    "player", r[1], "event", r[3], "current_status", currentStatus,
                    "message", "Confirm registration for " + r[1] + " in " + r[3] + "?");
        }

        int updated = em.createQuery(
                        "UPDATE SportsEventRegistration r SET r.status = 'CONFIRMED', " +
                        "r.updatedAt = CURRENT_TIMESTAMP WHERE r.id = :rid")
                .setParameter("rid", registrationId)
                .executeUpdate();

        log.info("Registration {} confirmed by admin user={}", registrationId, ctx.userId());
        return Map.of("success", updated > 0, "new_status", "CONFIRMED");
    }

    @Tool(description = "[ADMIN ONLY] Reject a pending registration with an optional reason. "
            + "Requires confirmation.")
    @Transactional
    public Object rejectRegistration(
            @ToolParam(description = "Registration ID") Long registrationId,
            @ToolParam(required = false, description = "Rejection reason") String reason,
            @ToolParam(description = "Has the user explicitly confirmed?") Boolean confirmed) {

        UserContext ctx = AgentSecurityContext.get();
        if (!ctx.isAdmin()) {
            return Map.of("error", true, "message", "Only admins can reject registrations.");
        }

        var rows = em.createQuery(
                        "SELECT COALESCE(r.playerName, u.fullName), e.name " +
                        "FROM SportsEventRegistration r JOIN r.event e LEFT JOIN r.user u " +
                        "WHERE r.id = :rid AND e.community.id = :comId", Object[].class)
                .setParameter("rid", registrationId)
                .setParameter("comId", ctx.communityId())
                .getResultList();

        if (rows.isEmpty()) return Map.of("error", "Registration not found in your community");

        if (!Boolean.TRUE.equals(confirmed)) {
            return Map.of("requires_confirmation", true,
                    "player", rows.get(0)[0], "event", rows.get(0)[1],
                    "message", "Reject registration for " + rows.get(0)[0] + "?");
        }

        em.createQuery(
                        "UPDATE SportsEventRegistration r SET r.status = 'REJECTED', " +
                        "r.updatedAt = CURRENT_TIMESTAMP WHERE r.id = :rid")
                .setParameter("rid", registrationId)
                .executeUpdate();

        log.info("Registration {} rejected by admin user={}, reason={}", registrationId, ctx.userId(), reason);
        return Map.of("success", true, "new_status", "REJECTED", "reason", reason != null ? reason : "");
    }
}
