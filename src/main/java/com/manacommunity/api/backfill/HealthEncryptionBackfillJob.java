package com.manacommunity.api.backfill;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * One-shot backfill that brings legacy plaintext health data under AES-256-GCM encryption.
 *
 * <h3>Why this job exists</h3>
 * V499 only added column comments; it never re-wrote the data, and a SQL migration cannot
 * do it because the ciphertext has to come from the application's cipher. Decryption was
 * then made tolerant of plaintext so existing rows keep reading — which means a row that is
 * never written again stays plaintext at rest forever. This job performs that write.
 *
 * <h3>Trigger</h3>
 * Never automatic. It runs only when a SUPER_ADMIN calls
 * {@code POST /api/admin/backfill/health-encryption} (see
 * {@link HealthEncryptionBackfillController}), and a mutating run additionally requires
 * {@code mana.backfill.health-encryption.enabled=true}. Dry runs need no flag.
 *
 * <h3>Safety properties</h3>
 * <ul>
 *   <li><b>Paged</b> — keyset pagination on {@code id}; the tables are never loaded whole.</li>
 *   <li><b>Idempotent</b> — rows already holding ciphertext are detected (see
 *       {@link EncryptedFieldInspector}) and skipped, so re-running cannot double-encrypt.</li>
 *   <li><b>Row-atomic on ambiguity</b> — Hibernate rewrites every column of a row, so if ANY
 *       sensitive field on a row is ambiguous the ENTIRE row is skipped and reported. Writing
 *       the row would otherwise re-wrap the ambiguous field.</li>
 *   <li><b>Batch transactions</b> — one transaction per batch
 *       ({@link HealthEncryptionBatchWriter}); a failure part-way leaves earlier batches
 *       committed and the job safe to re-run.</li>
 *   <li><b>No values logged</b> — only ids and counts.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthEncryptionBackfillJob {

    public static final String ENABLED_PROPERTY = "mana.backfill.health-encryption.enabled";

    /** Cap on how many ambiguous {@code table#id} references are returned to the caller. */
    static final int AMBIGUOUS_SAMPLE_LIMIT = 50;

    private static final String HEALTH_PROFILE_TABLE = "manacommunity.health_profile";
    private static final List<String> HEALTH_PROFILE_COLUMNS = List.of(
            "allergies", "medical_conditions", "emergency_notes",
            "medical_id_number", "physician_name", "physician_phone");

    private static final String EMERGENCY_CONTACT_TABLE = "manacommunity.emergency_contact";
    private static final List<String> EMERGENCY_CONTACT_COLUMNS = List.of("name", "phone", "email");

    private final JdbcTemplate jdbcTemplate;
    private final EncryptedFieldInspector inspector;
    private final HealthEncryptionBatchWriter writer;

    @Value("${" + ENABLED_PROPERTY + ":false}")
    private boolean enabled;

    @Value("${mana.backfill.health-encryption.batch-size:200}")
    private int batchSize;

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Runs the backfill.
     *
     * @param dryRun when true, rows are classified and counted but nothing is written
     */
    public HealthEncryptionBackfillReport run(boolean dryRun) {
        int effectiveBatch = batchSize > 0 ? batchSize : 200;
        log.info("Health-encryption backfill starting (dryRun={}, batchSize={})", dryRun, effectiveBatch);

        List<String> ambiguousRefs = new ArrayList<>();

        HealthEncryptionBackfillReport.TableResult profiles = processTable(
                HEALTH_PROFILE_TABLE, HEALTH_PROFILE_COLUMNS, dryRun, effectiveBatch,
                writer::rewriteHealthProfiles, ambiguousRefs);

        HealthEncryptionBackfillReport.TableResult contacts = processTable(
                EMERGENCY_CONTACT_TABLE, EMERGENCY_CONTACT_COLUMNS, dryRun, effectiveBatch,
                writer::rewriteEmergencyContacts, ambiguousRefs);

        List<HealthEncryptionBackfillReport.TableResult> tables = List.of(profiles, contacts);
        long examined = tables.stream().mapToLong(HealthEncryptionBackfillReport.TableResult::rowsExamined).sum();
        long encrypted = tables.stream().mapToLong(HealthEncryptionBackfillReport.TableResult::rowsEncrypted).sum();
        long ambiguous = tables.stream().mapToLong(HealthEncryptionBackfillReport.TableResult::rowsAmbiguous).sum();
        long failed = tables.stream().mapToLong(HealthEncryptionBackfillReport.TableResult::rowsFailed).sum();
        long skipped = examined - encrypted - failed;

        log.info("Health-encryption backfill finished (dryRun={}): examined={} encrypted={} skipped={} "
                        + "(of which ambiguous={}) failed={}",
                dryRun, examined, encrypted, skipped, ambiguous, failed);
        if (ambiguous > 0) {
            log.warn("{} row(s) were skipped as AMBIGUOUS: valid Base64 and long enough to be ciphertext, "
                    + "but the GCM tag did not verify under the current key. These may be ciphertext under a "
                    + "rotated key — they were NOT touched and need a human decision. Refs (max {}): {}",
                    ambiguous, AMBIGUOUS_SAMPLE_LIMIT, ambiguousRefs);
        }

        return new HealthEncryptionBackfillReport(
                dryRun, tables, examined, encrypted, skipped, ambiguous, failed, List.copyOf(ambiguousRefs));
    }

    private HealthEncryptionBackfillReport.TableResult processTable(
            String table,
            List<String> columns,
            boolean dryRun,
            int batch,
            ToIntFunction<List<Long>> batchRewriter,
            List<String> ambiguousRefs) {

        // Column names are compile-time constants, never caller input.
        String sql = "SELECT id, " + String.join(", ", columns)
                + " FROM " + table + " WHERE id > ? ORDER BY id ASC LIMIT ?";

        long examined = 0, needing = 0, rewritten = 0, already = 0, nothingToDo = 0, ambiguous = 0, failed = 0;
        long lastId = 0;
        int page = 0;

        while (true) {
            final long cursor = lastId;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, cursor, batch);
            if (rows.isEmpty()) {
                break;
            }
            page++;

            List<Long> toRewrite = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                long id = ((Number) row.get("id")).longValue();
                lastId = Math.max(lastId, id);
                examined++;

                Map<EncryptedFieldState, Integer> states = countStates(row, columns);
                if (states.getOrDefault(EncryptedFieldState.AMBIGUOUS, 0) > 0) {
                    ambiguous++;
                    if (ambiguousRefs.size() < AMBIGUOUS_SAMPLE_LIMIT) {
                        ambiguousRefs.add(table + "#" + id);
                    }
                    log.warn("Skipping {} id={} — {} sensitive field(s) could not be classified as either "
                                    + "plaintext or ciphertext under the current key; refusing to guess.",
                            table, id, states.get(EncryptedFieldState.AMBIGUOUS));
                } else if (states.getOrDefault(EncryptedFieldState.PLAINTEXT, 0) > 0) {
                    needing++;
                    toRewrite.add(id);
                } else if (states.getOrDefault(EncryptedFieldState.ENCRYPTED, 0) > 0) {
                    already++;
                } else {
                    nothingToDo++;
                }
            }

            if (!dryRun && !toRewrite.isEmpty()) {
                try {
                    rewritten += batchRewriter.applyAsInt(toRewrite);
                } catch (RuntimeException e) {
                    // The batch's own transaction has rolled back: these rows are untouched
                    // and the next run will pick them up again. Keep going.
                    failed += toRewrite.size();
                    log.error("Batch {} of {} failed to commit ({} row(s) left as-is, ids {}): {}",
                            page, table, toRewrite.size(), toRewrite, e.toString());
                }
            }

            log.info("{} backfill progress: page={} examined={} needingEncryption={} rewritten={} "
                            + "alreadyEncrypted={} ambiguous={} failed={} lastId={}",
                    table, page, examined, needing, rewritten, already, ambiguous, failed, lastId);

            if (rows.size() < batch) {
                break;
            }
        }

        return new HealthEncryptionBackfillReport.TableResult(
                table, examined, rewritten, needing, already, nothingToDo, ambiguous, failed);
    }

    private Map<EncryptedFieldState, Integer> countStates(Map<String, Object> row, List<String> columns) {
        Map<EncryptedFieldState, Integer> counts = new EnumMap<>(EncryptedFieldState.class);
        for (String column : columns) {
            Object value = row.get(column);
            String raw = value != null ? value.toString() : null;
            EncryptedFieldState state = inspector.classify(raw);
            counts.merge(state, 1, Integer::sum);
        }
        return counts;
    }
}
