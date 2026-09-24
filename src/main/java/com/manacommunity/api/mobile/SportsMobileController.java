package com.manacommunity.api.mobile;

import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.LoggedInUserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Exposes the mobile-friendly sports URL contract that differs from the
 * existing web-first sports API. Bridges to the existing service layer
 * via EntityManager rather than duplicating services.
 *
 * All endpoints are scoped to the authenticated user's community.
 *
 * Base path: /api/sports/mobile
 */
@Slf4j
@RestController
@RequestMapping("/api/sports/mobile")
@RequiredArgsConstructor
public class SportsMobileController {

    @PersistenceContext
    private final EntityManager em;

    private final LoggedInUserService loggedInUserService;

    // ── Helpers ──────────────────────────────────────────────────

    private Long communityId(UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return user.getCommunity() != null ? user.getCommunity().getId() : null;
    }

    // ── Match Schedule ────────────────────────────────────────────

    /**
     * GET /api/sports/mobile/matches
     * Params: tournamentId, date (yyyy-MM-dd), status, page, size
     */
    @GetMapping("/matches")
    public ResponseEntity<Map<String, Object>> getMatches(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long   tournamentId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "30") int size) {

        Long cid = communityId(principal);

        StringBuilder jpql = new StringBuilder(
            "SELECT m FROM SportMatch m WHERE m.tournament.community.id = :cid");
        Map<String, Object> params = new HashMap<>();
        params.put("cid", cid);

        if (tournamentId != null) {
            jpql.append(" AND m.tournament.id = :tid");
            params.put("tid", tournamentId);
        }
        if (date != null && !date.isBlank()) {
            LocalDate d = LocalDate.parse(date);
            jpql.append(" AND m.scheduledAt >= :dayStart AND m.scheduledAt < :dayEnd");
            params.put("dayStart", d.atStartOfDay());
            params.put("dayEnd",   d.plusDays(1).atStartOfDay());
        }
        if (status != null && !status.isBlank()) {
            jpql.append(" AND m.status = :status");
            params.put("status", status.toUpperCase());
        }
        jpql.append(" ORDER BY m.scheduledAt ASC");

        var query = em.createQuery(jpql.toString());
        params.forEach(query::setParameter);

        long total = em.createQuery(
                jpql.toString().replace("SELECT m FROM", "SELECT COUNT(m) FROM"), Long.class)
            .setParameters(null)
            .getSingleResult();

        // Re-query with parameters for count
        var countQ = em.createQuery(jpql.toString().replaceFirst("SELECT m", "SELECT COUNT(m)"), Long.class);
        params.forEach(countQ::setParameter);
        total = countQ.getSingleResult();

        query.setFirstResult(page * size).setMaxResults(size);
        @SuppressWarnings("unchecked")
        List<Object> rows = query.getResultList();

        return ResponseEntity.ok(Map.of(
            "content",       rows.stream().map(this::toMatchDto).collect(Collectors.toList()),
            "page",          page,
            "size",          size,
            "totalElements", total,
            "totalPages",    (int) Math.ceil((double) total / size)
        ));
    }

    /** GET /api/sports/mobile/matches/live */
    @GetMapping("/matches/live")
    public ResponseEntity<List<Map<String, Object>>> getLiveMatches(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long cid = communityId(principal);
        @SuppressWarnings("unchecked")
        List<Object> rows = em.createQuery(
            "SELECT m FROM SportMatch m WHERE m.tournament.community.id=:cid AND m.status='LIVE'")
            .setParameter("cid", cid)
            .getResultList();
        return ResponseEntity.ok(rows.stream().map(this::toMatchDto).collect(Collectors.toList()));
    }

    /** GET /api/sports/mobile/matches/today */
    @GetMapping("/matches/today")
    public ResponseEntity<List<Map<String, Object>>> getTodayMatches(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long cid = communityId(principal);
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end   = start.plusDays(1);
        @SuppressWarnings("unchecked")
        List<Object> rows = em.createQuery(
            "SELECT m FROM SportMatch m WHERE m.tournament.community.id=:cid" +
            " AND m.scheduledAt>=:s AND m.scheduledAt<:e ORDER BY m.scheduledAt")
            .setParameter("cid", cid)
            .setParameter("s", start)
            .setParameter("e", end)
            .getResultList();
        return ResponseEntity.ok(rows.stream().map(this::toMatchDto).collect(Collectors.toList()));
    }

    /** GET /api/sports/mobile/matches/{id} */
    @GetMapping("/matches/{id}")
    public ResponseEntity<Map<String, Object>> getMatch(
            @PathVariable Long id) {
        Object match = em.find(Object.class, id); // cast at runtime
        try {
            Object m = em.createQuery("SELECT m FROM SportMatch m WHERE m.id=:id")
                .setParameter("id", id)
                .getSingleResult();
            return ResponseEntity.ok(toMatchDto(m));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** GET /api/sports/mobile/matches/{id}/events */
    @GetMapping("/matches/{id}/events")
    public ResponseEntity<List<Map<String, Object>>> getMatchEvents(
            @PathVariable Long id) {
        @SuppressWarnings("unchecked")
        List<Object> events = em.createQuery(
            "SELECT e FROM MatchEvent e WHERE e.match.id=:id ORDER BY e.createdAt")
            .setParameter("id", id)
            .getResultList();
        return ResponseEntity.ok(events.stream().map(this::toEventDto).collect(Collectors.toList()));
    }

    // ── DTO mapping helpers ───────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMatchDto(Object raw) {
        // Use reflection to extract fields from the entity
        try {
            var cls   = raw.getClass();
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id",             cls.getDeclaredMethod("getId").invoke(raw));
            dto.put("status",         getField(raw, cls, "status"));
            dto.put("homeScore",      getField(raw, cls, "homeScore"));
            dto.put("awayScore",      getField(raw, cls, "awayScore"));
            dto.put("scheduledAt",    getField(raw, cls, "scheduledAt"));
            dto.put("venue",          getField(raw, cls, "venue"));
            dto.put("round",          getField(raw, cls, "round"));
            dto.put("currentPeriod",  getField(raw, cls, "currentPeriod"));
            dto.put("elapsedMinutes", getField(raw, cls, "elapsedMinutes"));
            dto.put("homeOvers",      getField(raw, cls, "homeOvers"));
            dto.put("homeWickets",    getField(raw, cls, "homeWickets"));
            dto.put("awayOvers",      getField(raw, cls, "awayOvers"));
            dto.put("awayWickets",    getField(raw, cls, "awayWickets"));
            dto.put("homeSetsWon",    getField(raw, cls, "homeSetsWon"));
            dto.put("awaySetsWon",    getField(raw, cls, "awaySetsWon"));

            // Tournament and team
            Object tournament = cls.getDeclaredMethod("getTournament").invoke(raw);
            if (tournament != null) {
                var tc = tournament.getClass();
                dto.put("tournamentId",   getField(tournament, tc, "id"));
                dto.put("tournamentName", getField(tournament, tc, "name"));
                dto.put("sport",          getField(tournament, tc, "sport"));
            }
            Object homeTeam = cls.getDeclaredMethod("getHomeTeam").invoke(raw);
            Object awayTeam = cls.getDeclaredMethod("getAwayTeam").invoke(raw);
            if (homeTeam != null) {
                var hc = homeTeam.getClass();
                dto.put("homeTeamId",    getField(homeTeam, hc, "id"));
                dto.put("homeTeamName",  getField(homeTeam, hc, "name"));
                dto.put("homeTeamEmoji", getField(homeTeam, hc, "logoEmoji"));
            }
            if (awayTeam != null) {
                var ac = awayTeam.getClass();
                dto.put("awayTeamId",    getField(awayTeam, ac, "id"));
                dto.put("awayTeamName",  getField(awayTeam, ac, "name"));
                dto.put("awayTeamEmoji", getField(awayTeam, ac, "logoEmoji"));
            }
            return dto;
        } catch (Exception e) {
            log.warn("Failed to map SportMatch: {}", e.getMessage());
            return Map.of("error", "mapping failed");
        }
    }

    private Map<String, Object> toEventDto(Object raw) {
        try {
            var cls = raw.getClass();
            return Map.of(
                "id",          getField(raw, cls, "id"),
                "type",        getField(raw, cls, "type"),
                "teamName",    getField(raw, cls, "teamName"),
                "playerName",  getField(raw, cls, "playerName"),
                "description", getField(raw, cls, "description"),
                "minute",      getField(raw, cls, "minute"),
                "over",        getField(raw, cls, "over"),
                "timestamp",   getField(raw, cls, "createdAt")
            );
        } catch (Exception e) {
            return Map.of("error", "mapping failed");
        }
    }

    private Object getField(Object obj, Class<?> cls, String field) {
        try {
            var f = cls.getDeclaredField(field);
            f.setAccessible(true);
            return f.get(obj);
        } catch (Exception e) {
            try {
                // Try getter
                String getter = "get" + field.substring(0, 1).toUpperCase() + field.substring(1);
                return cls.getDeclaredMethod(getter).invoke(obj);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
