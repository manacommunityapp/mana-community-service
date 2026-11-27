package com.manacommunity.api.db.migration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates zero-downtime online schema migration plans for high-throughput tables
 * (e.g. 800M-row visitor_entries table with 3,000 writes/sec), enforcing non-blocking
 * gh-ost binlog replication standards and preventing metadata lock queue pileups.
 */
@Slf4j
@Service
public class ZeroDowntimeMigrationSafetyValidator {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MigrationPlan {
        private String tableName;
        private long estimatedRowCount;
        private int peakWritesPerSecond;
        private String tool; // "GH_OST", "PT_ONLINE_SCHEMA_CHANGE", "NATIVE_ALTER"
        private int chunkSize;
        private long maxReplicaLagMillis;
        private int maxThreadsRunning;
        private int lockWaitTimeoutSeconds;
        private boolean useTriggers;
        private String cutoverMethod; // "ATOMIC_RENAME", "LOCK_AND_SWAP"
        private String defaultColumnClause;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationReport {
        private boolean isApproved;
        private List<String> criticalViolations;
        private List<String> warnings;
        private String executionMode;
    }

    public ValidationReport evaluateMigrationPlan(MigrationPlan plan) {
        List<String> violations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        boolean isHighVolume = plan.getEstimatedRowCount() > 10_000_000L || plan.getPeakWritesPerSecond() > 500;

        // Rule 1: Prohibit Native ALTER on High-Volume Tables
        if (isHighVolume && "NATIVE_ALTER".equalsIgnoreCase(plan.getTool())) {
            violations.add("Native ALTER TABLE prohibited on tables >10M rows or >500 writes/sec (causes table lock or online log overflow)");
        }

        // Rule 2: Prohibit Synchronous Triggers on High-Write Tables (3k writes/sec)
        if (plan.getPeakWritesPerSecond() > 1000 && (plan.isUseTriggers() || "PT_ONLINE_SCHEMA_CHANGE".equalsIgnoreCase(plan.getTool()))) {
            violations.add(String.format("Trigger-based migrations (pt-online-schema-change) prohibited at %d writes/sec: triggers induce write amplification and replica lag",
                    plan.getPeakWritesPerSecond()));
        }

        // Rule 3: Enforce gh-ost Binlog Streaming for ultra-high write tables
        if (isHighVolume && !"GH_OST".equalsIgnoreCase(plan.getTool())) {
            violations.add("gh-ost (triggerless binlog streaming) is strictly required for zero-downtime execution on this table");
        }

        // Rule 4: Chunk size validation (prevent long-running copy transactions)
        if (plan.getChunkSize() <= 0 || plan.getChunkSize() > 2500) {
            violations.add(String.format("Chunk size (%d) exceeds safe bounds [100 - 2,500]; large chunks cause replication lag", plan.getChunkSize()));
        }

        // Rule 5: Replica lag threshold must not exceed 1500ms
        if (plan.getMaxReplicaLagMillis() <= 0 || plan.getMaxReplicaLagMillis() > 1500) {
            violations.add(String.format("Max replica lag threshold (%d ms) exceeds 1,500 ms limit; risks replica failover", plan.getMaxReplicaLagMillis()));
        }

        // Rule 6: Lock wait timeout must be bounded (<= 3 seconds)
        if (plan.getLockWaitTimeoutSeconds() > 3) {
            violations.add(String.format("Lock wait timeout (%ds) exceeds 3 seconds; risks piling up metadata lock queue and connection pool starvation",
                    plan.getLockWaitTimeoutSeconds()));
        }

        // Rule 7: Cutover must be atomic rename
        if (!"ATOMIC_RENAME".equalsIgnoreCase(plan.getCutoverMethod())) {
            violations.add("Cutover method must be ATOMIC_RENAME (two-table rename in a single statement) to ensure zero dropped queries");
        }

        // Warning: Non-null column addition without explicit DEFAULT
        if (plan.getDefaultColumnClause() == null || plan.getDefaultColumnClause().isBlank()) {
            warnings.add("Adding a NOT NULL column without DEFAULT requires backfilling defaults on 800M historical rows; ensure backfill script is staged");
        }

        boolean approved = violations.isEmpty();
        String mode = approved ? "PRODUCTION_READY_NON_BLOCKING" : "BLOCKED_HIGH_RISK";

        return ValidationReport.builder()
                .isApproved(approved)
                .criticalViolations(violations)
                .warnings(warnings)
                .executionMode(mode)
                .build();
    }
}
