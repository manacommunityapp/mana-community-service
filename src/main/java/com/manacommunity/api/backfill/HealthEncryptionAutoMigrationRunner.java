package com.manacommunity.api.backfill;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Runs the health data encryption backfill migration on application startup
 * to bring historical plaintext records under AES-256-GCM encryption.
 * Idempotent: already encrypted records are inspected and skipped automatically.
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mana.backfill.health-encryption.auto-run", havingValue = "true", matchIfMissing = true)
public class HealthEncryptionAutoMigrationRunner implements ApplicationRunner {

    private final HealthEncryptionBackfillJob job;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Executing health data encryption migration for historical plaintext records...");
        try {
            var report = job.run(false);
            log.info("Health data encryption migration finished: examined={}, encrypted={}, skipped={}, failed={}",
                    report.rowsExamined(), report.rowsEncrypted(), report.rowsSkipped(), report.rowsFailed());
        } catch (Exception e) {
            log.warn("Health data encryption migration run deferred or encountered issue: {}", e.getMessage());
        }
    }
}
