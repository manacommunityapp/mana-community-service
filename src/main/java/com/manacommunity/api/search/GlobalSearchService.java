package com.manacommunity.api.search;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Cross-module keyword search scoped to a community.
 * Uses native SQL ILIKE queries (PostgreSQL) with LIMIT 5 per type.
 */
@Service
@Transactional(readOnly = true)
public class GlobalSearchService {

    private static final int PER_TYPE_LIMIT = 5;
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 50;

    /**
     * Frontend route templates per result type. These are the ONLY source of the
     * {@code url} field: the id is formatted in as a number, so no user-controlled
     * text can reach the path.
     */
    private static final String URL_RESIDENT    = "/residents/%d";
    private static final String URL_MAINTENANCE = "/maintenance/tickets/%d";
    private static final String URL_EVENT       = "/events/%d";
    private static final String URL_NOTICE      = "/notices/%d";
    private static final String URL_VISITOR     = "/security/visitors/%d";

    @PersistenceContext
    private EntityManager em;

    /**
     * Search across Residents, Maintenance (Tickets), Events, Notices, and Visitors.
     *
     * @param query       keyword; wrapped in %…% for ILIKE
     * @param communityId tenant scope
     * @return merged list, up to {@code limit} items (default 20)
     */
    public List<GlobalSearchResult> search(String query, Long communityId, int limit) {
        if (query == null || query.isBlank()) return List.of();
        // A negative (or absurdly large) limit used to blow up on subList with an
        // IndexOutOfBoundsException -> HTTP 500. Clamp it to a sane window.
        int effectiveLimit = Math.max(MIN_LIMIT, Math.min(limit, MAX_LIMIT));
        String pattern = "%" + query.toLowerCase() + "%";
        List<GlobalSearchResult> results = new ArrayList<>();

        results.addAll(searchResidents(pattern, communityId));
        results.addAll(searchMaintenanceTickets(pattern, communityId));
        results.addAll(searchEvents(pattern, communityId));
        results.addAll(searchNotices(pattern, communityId));
        results.addAll(searchVisitors(pattern, communityId));

        return results.size() <= effectiveLimit ? results : results.subList(0, effectiveLimit);
    }

    // ── per-type queries ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<GlobalSearchResult> searchResidents(String pattern, Long communityId) {
        String sql = """
                SELECT id, full_name, flat_no
                FROM app_user
                WHERE community_id = :cid
                  AND (LOWER(full_name) ILIKE :q
                       OR LOWER(email)   ILIKE :q
                       OR LOWER(flat_no) ILIKE :q
                       OR LOWER(phone)   ILIKE :q)
                LIMIT :lim
                """;
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("cid", communityId)
                .setParameter("q", pattern)
                .setParameter("lim", PER_TYPE_LIMIT)
                .getResultList();

        return rows.stream().map(r -> new GlobalSearchResult(
                "RESIDENT",
                toLong(r[0]),
                str(r[1]),
                str(r[2]),
                url(URL_RESIDENT, toLong(r[0]))
        )).toList();
    }

    @SuppressWarnings("unchecked")
    private List<GlobalSearchResult> searchMaintenanceTickets(String pattern, Long communityId) {
        String sql = """
                SELECT t.id, t.subject, t.status
                FROM ticket t
                WHERE t.community_id = :cid
                  AND (LOWER(t.subject)     ILIKE :q
                       OR LOWER(t.description) ILIKE :q
                       OR LOWER(t.ticket_number) ILIKE :q)
                ORDER BY t.created_at DESC
                LIMIT :lim
                """;
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("cid", communityId)
                .setParameter("q", pattern)
                .setParameter("lim", PER_TYPE_LIMIT)
                .getResultList();

        return rows.stream().map(r -> new GlobalSearchResult(
                "MAINTENANCE",
                toLong(r[0]),
                str(r[1]),
                str(r[2]),
                url(URL_MAINTENANCE, toLong(r[0]))
        )).toList();
    }

    @SuppressWarnings("unchecked")
    private List<GlobalSearchResult> searchEvents(String pattern, Long communityId) {
        String sql = """
                SELECT e.id, e.title, e.status
                FROM event_community e
                WHERE e.community_id = :cid
                  AND (LOWER(e.title)          ILIKE :q
                       OR LOWER(e.organizer_name) ILIKE :q)
                ORDER BY e.start_date DESC
                LIMIT :lim
                """;
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("cid", communityId)
                .setParameter("q", pattern)
                .setParameter("lim", PER_TYPE_LIMIT)
                .getResultList();

        return rows.stream().map(r -> new GlobalSearchResult(
                "EVENT",
                toLong(r[0]),
                str(r[1]),
                str(r[2]),
                url(URL_EVENT, toLong(r[0]))
        )).toList();
    }

    @SuppressWarnings("unchecked")
    private List<GlobalSearchResult> searchNotices(String pattern, Long communityId) {
        String sql = """
                SELECT n.id, n.title, n.category
                FROM notice n
                WHERE n.community_id = :cid
                  AND (LOWER(n.title)   ILIKE :q
                       OR LOWER(n.content) ILIKE :q)
                ORDER BY n.created_at DESC
                LIMIT :lim
                """;
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("cid", communityId)
                .setParameter("q", pattern)
                .setParameter("lim", PER_TYPE_LIMIT)
                .getResultList();

        return rows.stream().map(r -> new GlobalSearchResult(
                "NOTICE",
                toLong(r[0]),
                str(r[1]),
                str(r[2]),
                url(URL_NOTICE, toLong(r[0]))
        )).toList();
    }

    @SuppressWarnings("unchecked")
    private List<GlobalSearchResult> searchVisitors(String pattern, Long communityId) {
        String sql = """
                SELECT v.id, v.visitor_name, v.pass_code
                FROM visitor_pass v
                WHERE v.community_id = :cid
                  AND (LOWER(v.visitor_name)  ILIKE :q
                       OR LOWER(v.visitor_phone) ILIKE :q
                       OR LOWER(v.pass_code)     ILIKE :q
                       OR LOWER(v.flat_number)   ILIKE :q)
                ORDER BY v.created_at DESC
                LIMIT :lim
                """;
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("cid", communityId)
                .setParameter("q", pattern)
                .setParameter("lim", PER_TYPE_LIMIT)
                .getResultList();

        return rows.stream().map(r -> new GlobalSearchResult(
                "VISITOR",
                toLong(r[0]),
                str(r[1]),
                str(r[2]),
                url(URL_VISITOR, toLong(r[0]))
        )).toList();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds the relative frontend route for a hit. Returns null when the id is
     * missing (nothing sensible to navigate to) so the clients can skip the link
     * rather than follow a malformed path.
     */
    private static String url(String template, Long id) {
        return id == null ? null : String.format(template, id);
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(o.toString());
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
