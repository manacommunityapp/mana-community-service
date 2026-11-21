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
        String pattern = "%" + query.toLowerCase() + "%";
        List<GlobalSearchResult> results = new ArrayList<>();

        results.addAll(searchResidents(pattern, communityId));
        results.addAll(searchMaintenanceTickets(pattern, communityId));
        results.addAll(searchEvents(pattern, communityId));
        results.addAll(searchNotices(pattern, communityId));
        results.addAll(searchVisitors(pattern, communityId));

        return results.size() <= limit ? results : results.subList(0, limit);
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
                communityId
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
                communityId
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
                communityId
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
                communityId
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
                communityId
        )).toList();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(o.toString());
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
