package com.manacommunity.api.finance.ledger;

import com.manacommunity.api.platform.web.ApiResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only diagnostics for double-entry ledger integrity.
 *
 * <p>V504 tries to create a UNIQUE index on
 * {@code (community_id, reference_type, reference_id, account_code)} so one source
 * document cannot post twice. If the table already held duplicates, creating it would
 * fail the migration and block startup, so V504 catches that, logs a WARNING and
 * creates a plain index instead. The practical problem is that the warning scrolls
 * past in a Flyway log and nothing afterwards says the database is in the degraded
 * state — idempotency is then enforced only in application code.
 *
 * <p>This endpoint answers both questions: did the fallback fire, and if so exactly
 * which rows need attention. It deliberately does not delete anything. Which of a
 * duplicated pair is the real posting is an accounting decision, not a migration.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/ledger")
public class LedgerIntegrityController {

    /** Cap so a badly duplicated table cannot produce an unbounded response. */
    private static final int MAX_GROUPS = 200;

    @PersistenceContext
    private EntityManager em;

    public record DuplicateGroup(
            Long communityId,
            String referenceType,
            String referenceId,
            String accountCode,
            long rowCount,
            String entryIds
    ) {}

    public record LedgerIntegrityReport(
            boolean uniqueIndexPresent,
            boolean fallbackIndexPresent,
            String verdict,
            int groupsReturned,
            boolean truncated,
            List<DuplicateGroup> duplicateGroups
    ) {}

    @GetMapping("/reference-duplicates")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<LedgerIntegrityReport>> referenceDuplicates(
            @RequestParam(defaultValue = "200") int limit) {

        int cap = Math.max(1, Math.min(limit, MAX_GROUPS));

        boolean unique = indexExists("ux_gle_reference_account");
        boolean fallback = indexExists("idx_gle_reference_account");

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT community_id,
                       reference_type,
                       reference_id,
                       account_code,
                       COUNT(*) AS row_count,
                       STRING_AGG(id::TEXT, ',' ORDER BY id) AS entry_ids
                  FROM general_ledger_entry
                 WHERE reference_type IS NOT NULL
                   AND reference_id   IS NOT NULL
                 GROUP BY community_id, reference_type, reference_id, account_code
                HAVING COUNT(*) > 1
                 ORDER BY COUNT(*) DESC, community_id
                 LIMIT :lim
                """)
                .setParameter("lim", cap + 1)
                .getResultList();

        boolean truncated = rows.size() > cap;
        List<DuplicateGroup> groups = rows.stream()
                .limit(cap)
                .map(r -> new DuplicateGroup(
                        r[0] == null ? null : ((Number) r[0]).longValue(),
                        str(r[1]), str(r[2]), str(r[3]),
                        r[4] == null ? 0L : ((Number) r[4]).longValue(),
                        str(r[5])))
                .toList();

        String verdict;
        if (unique) {
            // The unique index exists, so duplicates are impossible by construction.
            verdict = "Unique index present — the ledger cannot hold duplicate postings.";
        } else if (groups.isEmpty()) {
            verdict = "No duplicates found, but the unique index is missing. "
                    + "It is now safe to create ux_gle_reference_account manually.";
        } else {
            verdict = "Duplicate postings present and the unique index was NOT created. "
                    + "Idempotency is enforced only in application code until these are resolved.";
        }

        if (!unique) {
            log.warn("Ledger integrity check: unique index absent (fallback index present={}), {} duplicate group(s) found",
                    fallback, groups.size());
        }

        return ResponseEntity.ok(ApiResponse.ok(new LedgerIntegrityReport(
                unique, fallback, verdict, groups.size(), truncated, groups)));
    }

    private boolean indexExists(String indexName) {
        Object n = em.createNativeQuery(
                        "SELECT COUNT(*) FROM pg_indexes WHERE indexname = :name")
                .setParameter("name", indexName)
                .getSingleResult();
        return n instanceof Number num && num.longValue() > 0;
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
