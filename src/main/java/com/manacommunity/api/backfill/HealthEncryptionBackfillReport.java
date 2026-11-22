package com.manacommunity.api.backfill;

import java.util.List;

/**
 * Outcome of one run of {@link HealthEncryptionBackfillJob}.
 *
 * @param dryRun            true when nothing was written
 * @param tables            per-table counters
 * @param rowsExamined      total rows read across all tables
 * @param rowsEncrypted     total rows re-written so the converter encrypted them
 * @param rowsSkipped       total rows left untouched (already encrypted, empty, or ambiguous)
 * @param rowsAmbiguous     subset of {@code rowsSkipped} that needs a human decision
 * @param rowsFailed        rows whose batch failed to commit (see the logs)
 * @param ambiguousRowRefs  up to {@link HealthEncryptionBackfillJob#AMBIGUOUS_SAMPLE_LIMIT}
 *                          {@code table#id} references for the ambiguous rows. Never values.
 */
public record HealthEncryptionBackfillReport(
        boolean dryRun,
        List<TableResult> tables,
        long rowsExamined,
        long rowsEncrypted,
        long rowsSkipped,
        long rowsAmbiguous,
        long rowsFailed,
        List<String> ambiguousRowRefs) {

    /**
     * @param table           table name
     * @param rowsExamined    rows read
     * @param rowsEncrypted   rows re-written (0 on a dry run — see {@code rowsNeedingEncryption})
     * @param rowsNeedingEncryption rows found to hold at least one plaintext field
     * @param rowsAlreadyEncrypted  rows whose every non-null sensitive field verified as ciphertext
     * @param rowsNothingToDo rows where every sensitive field is NULL
     * @param rowsAmbiguous   rows skipped because at least one field was ambiguous
     * @param rowsFailed      rows in a batch that failed to commit
     */
    public record TableResult(
            String table,
            long rowsExamined,
            long rowsEncrypted,
            long rowsNeedingEncryption,
            long rowsAlreadyEncrypted,
            long rowsNothingToDo,
            long rowsAmbiguous,
            long rowsFailed) {
    }
}
