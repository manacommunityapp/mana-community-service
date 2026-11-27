package com.manacommunity.api.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Database-backed distributed lock manager for multi-instance scheduler coordination.
 * Guarantees that only ONE node executes a scheduled batch cron job, even when multiple
 * replicas wake up simultaneously.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedJobLockService {

    private final JdbcTemplate jdbcTemplate;

    private static final String NODE_IDENTIFIER = resolveNodeId();

    private static String resolveNodeId() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID().toString().substring(0, 8);
        } catch (Exception e) {
            return "node-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }

    /**
     * Executes a task only if this instance successfully acquires the distributed lease.
     *
     * @param jobName Unique identifier for the scheduled job (e.g., "daily_basket_generation")
     * @param lockDuration How long the lock remains valid if the node crashes
     * @param task The task to execute
     * @return true if acquired and executed; false if skipped because another node holds the lock
     */
    public boolean executeWithLock(String jobName, Duration lockDuration, Runnable task) {
        if (!acquireLock(jobName, lockDuration)) {
            log.debug("Distributed job '{}' skipped — lease held by another instance.", jobName);
            return false;
        }

        try {
            log.info("Acquired distributed lease for '{}' on node '{}'. Executing task...", jobName, NODE_IDENTIFIER);
            task.run();
            return true;
        } catch (Exception e) {
            log.error("Error executing distributed job '{}': {}", jobName, e.getMessage(), e);
            throw e;
        } finally {
            releaseLock(jobName);
        }
    }

    /**
     * Attempts atomic lease acquisition using PostgreSQL INSERT ON CONFLICT DO UPDATE.
     */
    public boolean acquireLock(String jobName, Duration lockDuration) {
        Instant now = Instant.now();
        Instant lockUntil = now.plus(lockDuration);

        String sql = """
            INSERT INTO manacommunity.scheduled_job_lock (job_name, locked_at, locked_until, locked_by)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (job_name) DO UPDATE
            SET locked_at = EXCLUDED.locked_at,
                locked_until = EXCLUDED.locked_until,
                locked_by = EXCLUDED.locked_by
            WHERE manacommunity.scheduled_job_lock.locked_until < ?
            """;

        try {
            Timestamp nowTs = Timestamp.from(now);
            Timestamp lockUntilTs = Timestamp.from(lockUntil);
            int updated = jdbcTemplate.update(sql, jobName, nowTs, lockUntilTs, NODE_IDENTIFIER, nowTs);
            return updated > 0;
        } catch (Exception e) {
            log.warn("Failed to acquire distributed lock for '{}': {}", jobName, e.getMessage());
            return false;
        }
    }

    /**
     * Releases the lease by setting locked_until to now.
     */
    public void releaseLock(String jobName) {
        String sql = """
            UPDATE manacommunity.scheduled_job_lock
            SET locked_until = NOW()
            WHERE job_name = ? AND locked_by = ?
            """;
        try {
            jdbcTemplate.update(sql, jobName, NODE_IDENTIFIER);
            log.debug("Released distributed lease for '{}' on node '{}'", jobName, NODE_IDENTIFIER);
        } catch (Exception e) {
            log.warn("Could not explicitly release lock for '{}': {}", jobName, e.getMessage());
        }
    }
}
