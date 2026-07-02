package com.manacommunity.api.ai.tool;

import com.manacommunity.api.ai.config.AgentSecurityContext;
import com.manacommunity.api.ai.config.AgentSecurityContext.UserContext;
import com.manacommunity.api.dto.SportsEventRequest;
import com.manacommunity.api.service.SportsEventService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI tools that let admins create sports events via natural language instead
 * of filling a 20+ field form.
 *
 * <p>The flow is:</p>
 * <ol>
 *   <li>{@code resolveSportByName} — maps "badminton" → sport ID</li>
 *   <li>{@code resolveVenueByName} — maps "Sunrise Courts" → venue ID</li>
 *   <li>{@code createEventFromDescription} — assembles all parameters and calls
 *       {@link SportsEventService#createEvent} after confirmation</li>
 * </ol>
 *
 * <p>The AI agent calls these tools sequentially during a conversation,
 * resolving ambiguities by asking the user ("Did you mean Badminton or
 * Badminton Doubles?").</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventCreationTools {

    @PersistenceContext
    private EntityManager em;

    private final SportsEventService sportsEventService;

    // ── RESOLVERS ──────────────────────────────────────────────────────

    @Tool(description = "Resolve a sport name to its database ID. Used before creating an event. "
            + "Returns matching sports with IDs and formats so the AI can pick the right one.")
    public Object resolveSportByName(
            @ToolParam(description = "Sport name (partial match OK, e.g. 'cricket', 'badminton')")
            String sportName) {

        UserContext ctx = AgentSecurityContext.get();

        return em.createQuery(
                "SELECT s.id, s.name, s.formats FROM SportsMeta s " +
                "WHERE LOWER(s.name) LIKE LOWER(:nm) AND s.active = true " +
                "AND (s.communityId IS NULL OR s.communityId = :comId)", Object[].class)
                .setParameter("nm", "%" + sportName + "%")
                .setParameter("comId", ctx.communityId())
                .getResultList().stream()
                .map(r -> Map.of("sport_id", r[0], "name", r[1], "formats", r[2] != null ? r[2] : ""))
                .collect(Collectors.toList());
    }

    @Tool(description = "Resolve a venue name to its database ID. Used before creating an event. "
            + "Returns matching venues with IDs, addresses, and court counts.")
    public Object resolveVenueByName(
            @ToolParam(description = "Venue name (partial match OK, e.g. 'sunrise', 'central park')")
            String venueName) {

        UserContext ctx = AgentSecurityContext.get();

        var venues = em.createQuery(
                "SELECT v.id, v.name, v.address, v.city FROM Venue v " +
                "WHERE v.community.id = :comId AND LOWER(v.name) LIKE LOWER(:nm) " +
                "ORDER BY v.name", Object[].class)
                .setParameter("comId", ctx.communityId())
                .setParameter("nm", "%" + venueName + "%")
                .getResultList();

        List<Map<String, Object>> results = new ArrayList<>();
        for (Object[] v : venues) {
            Long courtCount = em.createQuery(
                    "SELECT COUNT(c) FROM Court c WHERE c.venue.id = :vid", Long.class)
                    .setParameter("vid", v[0]).getSingleResult();

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("venue_id", v[0]);
            m.put("name", v[1]);
            m.put("address", v[2]);
            m.put("city", v[3]);
            m.put("courts", courtCount);
            results.add(m);
        }
        return results;
    }

    @Tool(description = "Resolve player category names to IDs for event registration. "
            + "Returns matching categories with IDs, age ranges, and gender info.")
    public Object resolveCategoriesByName(
            @ToolParam(description = "Category name or type (e.g. 'mens', 'under 14', 'seniors')")
            String categoryQuery) {

        UserContext ctx = AgentSecurityContext.get();

        return em.createQuery(
                "SELECT c.id, c.name, c.category_type, c.minAge, c.maxAge, c.gender " +
                "FROM PlayerCategory c " +
                "WHERE (c.type = 'DEFAULT' OR c.community.id = :comId) " +
                "AND (LOWER(c.name) LIKE LOWER(:q) OR LOWER(c.category_type) LIKE LOWER(:q))",
                Object[].class)
                .setParameter("comId", ctx.communityId())
                .setParameter("q", "%" + categoryQuery + "%")
                .getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("category_id", r[0]);
                    m.put("name", r[1]);
                    m.put("type", r[2]);
                    m.put("age_range", (r[3] != null ? r[3] : "any") + "-" + (r[4] != null ? r[4] : "any"));
                    m.put("gender", r[5]);
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ── EVENT CREATION ─────────────────────────────────────────────────

    @Tool(description = "[ADMIN ONLY] Create a sports event from structured parameters. "
            + "Call resolveSportByName and resolveVenueByName first to get the IDs. "
            + "Requires explicit user confirmation before creating. "
            + "Returns a preview of the event to be created.")
    @Transactional
    public Object createEventFromDescription(
            @ToolParam(description = "Event name/title") String name,
            @ToolParam(description = "Sport ID (from resolveSportByName)") Long sportId,
            @ToolParam(description = "Event start date (YYYY-MM-DD)") String eventDateStart,
            @ToolParam(description = "Event end date (YYYY-MM-DD)") String eventDateEnd,
            @ToolParam(required = false, description = "Venue ID (from resolveVenueByName)") Long venueId,
            @ToolParam(required = false, description = "Max participants/teams") Integer maxParticipants,
            @ToolParam(required = false, description = "Format: SINGLES, DOUBLES, MIXED_DOUBLES, TEAM")
            String format,
            @ToolParam(required = false, description = "Tournament type: KNOCKOUT, ROUND_ROBIN, GROUP_PLAYOFF, SWISS")
            String tournamentType,
            @ToolParam(required = false, description = "Category IDs (from resolveCategoriesByName)")
            List<Long> categoryIds,
            @ToolParam(required = false, description = "Gender filter: MALE, FEMALE, ALL") String gender,
            @ToolParam(required = false, description = "Minimum age") Integer minAge,
            @ToolParam(required = false, description = "Maximum age") Integer maxAge,
            @ToolParam(required = false, description = "Registration start date (YYYY-MM-DD)")
            String registrationDateStart,
            @ToolParam(required = false, description = "Registration end date (YYYY-MM-DD)")
            String registrationDateEnd,
            @ToolParam(required = false, description = "Description") String description,
            @ToolParam(required = false, description = "Require admin approval for registrations? (default true)")
            Boolean adminApprovalRequired,
            @ToolParam(required = false, description = "Enable auction for this event?") Boolean auctionEnabled,
            @ToolParam(description = "Has the user explicitly confirmed this creation?") Boolean confirmed) {

        UserContext ctx = AgentSecurityContext.get();
        if (!ctx.isAdmin()) {
            return Map.of("error", true, "message", "Only admins can create events.");
        }

        // Build a preview
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("name", name);

        // Resolve sport name for preview
        var sportRows = em.createQuery(
                "SELECT s.name FROM SportsMeta s WHERE s.id = :sid", String.class)
                .setParameter("sid", sportId).getResultList();
        preview.put("sport", sportRows.isEmpty() ? "ID:" + sportId : sportRows.get(0));

        preview.put("dates", eventDateStart + " to " + eventDateEnd);
        if (venueId != null) {
            var venueRows = em.createQuery(
                    "SELECT v.name FROM Venue v WHERE v.id = :vid", String.class)
                    .setParameter("vid", venueId).getResultList();
            preview.put("venue", venueRows.isEmpty() ? "ID:" + venueId : venueRows.get(0));
        }
        preview.put("max_participants", maxParticipants);
        preview.put("format", format);
        preview.put("tournament_type", tournamentType);
        preview.put("gender", gender);
        preview.put("age_range", (minAge != null ? minAge : "any") + "-" + (maxAge != null ? maxAge : "any"));
        preview.put("admin_approval", adminApprovalRequired != null ? adminApprovalRequired : true);
        preview.put("auction_enabled", auctionEnabled);

        if (!Boolean.TRUE.equals(confirmed)) {
            return Map.of("requires_confirmation", true, "preview", preview,
                    "message", "Create this event? Review the preview above and confirm.");
        }

        // Build the request DTO
        SportsEventRequest req = new SportsEventRequest();
        req.setName(name);
        req.setSportId(sportId);
        req.setCommunityId(ctx.communityId());
        req.setEventDateStart(LocalDate.parse(eventDateStart));
        req.setEventDateEnd(LocalDate.parse(eventDateEnd));
        req.setVenueId(venueId);
        req.setMaxParticipants(maxParticipants);
        req.setFormat(format);
        req.setTournamentType(tournamentType);
        req.setCategoryIds(categoryIds);
        req.setGender(gender);
        req.setMinAge(minAge);
        req.setMaxAge(maxAge);
        req.setDescription(description);
        req.setAdminApprovalRequired(adminApprovalRequired != null ? adminApprovalRequired : true);
        req.setAuctionEnabled(auctionEnabled);

        if (registrationDateStart != null) req.setRegistrationDateStart(LocalDate.parse(registrationDateStart));
        if (registrationDateEnd != null) req.setRegistrationDateEnd(LocalDate.parse(registrationDateEnd));

        try {
            var event = sportsEventService.createEvent(req, ctx.userId());
            log.info("Event '{}' (ID:{}) created by admin user={} via AI agent",
                    name, event.getId(), ctx.userId());

            return Map.of("success", true,
                    "event_id", event.getId(),
                    "event_name", name,
                    "message", "Event '" + name + "' created successfully!");
        } catch (Exception e) {
            log.error("Event creation failed: {}", e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}
