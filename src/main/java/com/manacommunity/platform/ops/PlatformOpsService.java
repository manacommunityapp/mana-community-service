package com.manacommunity.platform.ops;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformOpsService {

    private final PlatformMetricsRepository metricsRepo;
    private final PlatformAlertRepository alertRepo;
    private final PlatformDeployRepository deployRepo;
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private final SimpMessagingTemplate messaging;

    // ─── Full Snapshot ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PlatformOpsSnapshot getSnapshot() {
        return new PlatformOpsSnapshot(
            Instant.now(),
            getApiHealth(),
            getJobStats(),
            getDbPool(),
            getNotificationStats(),
            getPaymentHealth(),
            getWsStats(),
            getStorageStats(),
            getRecentDeploys(10),
            getOpenAlerts(),
            getSecurityIncidents(24),
            getTenantHealth()
        );
    }

    // ─── Individual Domain Metrics ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public PlatformOpsSnapshot.ApiHealthMetrics getApiHealth() {
        try {
            var p95opt = metricsRepo.findLatestByType("API_LATENCY_P95");
            var p99opt = metricsRepo.findLatestByType("API_LATENCY_P99");
            var errOpt = metricsRepo.findLatestByType("API_ERROR_RATE");
            long p95 = p95opt.map(m -> m.getValue().longValue()).orElse(0L);
            long p99 = p99opt.map(m -> m.getValue().longValue()).orElse(0L);
            double errRate = errOpt.map(m -> m.getValue().doubleValue()).orElse(0.0);
            String status = errRate > 5.0 ? "DEGRADED" : (errRate > 1.0 ? "WARNING" : "HEALTHY");
            return new PlatformOpsSnapshot.ApiHealthMetrics(status, p95 / 2, p95, p99, errRate, 0L);
        } catch (Exception e) {
            log.warn("API health metrics unavailable: {}", e.getMessage());
            return PlatformOpsSnapshot.ApiHealthMetrics.healthy();
        }
    }

    @Transactional(readOnly = true)
    public PlatformOpsSnapshot.BackgroundJobStats getJobStats() {
        try {
            long failed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE status = 'FAILED' " +
                "AND created_at > NOW() - INTERVAL '24 hours'",
                Long.class);
            long pending = jdbc.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE status = 'PENDING'",
                Long.class);
            long failedTotal = jdbc.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE status = 'FAILED'",
                Long.class);
            return new PlatformOpsSnapshot.BackgroundJobStats(1, failed, pending, failedTotal);
        } catch (Exception e) {
            log.warn("Job stats unavailable: {}", e.getMessage());
            return new PlatformOpsSnapshot.BackgroundJobStats(0, 0, 0, 0);
        }
    }

    public PlatformOpsSnapshot.DbPoolMetrics getDbPool() {
        try {
            if (dataSource instanceof HikariDataSource hds) {
                HikariPoolMXBean pool = hds.getHikariPoolMXBean();
                if (pool != null) {
                    return new PlatformOpsSnapshot.DbPoolMetrics(
                        pool.getActiveConnections(),
                        pool.getIdleConnections(),
                        hds.getMaximumPoolSize(),
                        pool.getThreadsAwaitingConnection(),
                        0.0
                    );
                }
            }
        } catch (Exception e) {
            log.warn("DB pool stats unavailable: {}", e.getMessage());
        }
        return new PlatformOpsSnapshot.DbPoolMetrics(0, 0, 10, 0, 0.0);
    }

    @Transactional(readOnly = true)
    public PlatformOpsSnapshot.NotificationStats getNotificationStats() {
        try {
            long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE created_at > NOW() - INTERVAL '24 hours'",
                Long.class);
            long failed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE status = 'FAILED' " +
                "AND created_at > NOW() - INTERVAL '24 hours'",
                Long.class);
            double rate = total > 0 ? (double) failed / total * 100.0 : 0.0;

            // breakdown by aggregate_type as channel proxy
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT aggregate_type, COUNT(*) AS cnt FROM outbox_event " +
                "WHERE status = 'FAILED' AND created_at > NOW() - INTERVAL '24 hours' " +
                "GROUP BY aggregate_type");
            Map<String, Long> breakdown = rows.stream().collect(
                Collectors.toMap(r -> (String) r.get("aggregate_type"), r -> (Long) r.get("cnt")));

            return new PlatformOpsSnapshot.NotificationStats(total, failed, rate, breakdown);
        } catch (Exception e) {
            log.warn("Notification stats unavailable: {}", e.getMessage());
            return new PlatformOpsSnapshot.NotificationStats(0, 0, 0.0, Map.of());
        }
    }

    @Transactional(readOnly = true)
    public PlatformOpsSnapshot.PaymentHealthStats getPaymentHealth() {
        try {
            // wallet transactions pending > 30 min
            long stalled = jdbc.queryForObject(
                "SELECT COUNT(*) FROM community_wallet_transactions " +
                "WHERE status = 'PENDING' AND initiated_at < NOW() - INTERVAL '30 minutes'",
                Long.class);
            long pending = jdbc.queryForObject(
                "SELECT COUNT(*) FROM community_wallet_transactions WHERE status = 'PENDING'",
                Long.class);
            long failedTxn = jdbc.queryForObject(
                "SELECT COUNT(*) FROM community_wallet_transactions WHERE status = 'FAILED' " +
                "AND initiated_at > NOW() - INTERVAL '24 hours'",
                Long.class);
            return new PlatformOpsSnapshot.PaymentHealthStats(pending, 0.0, stalled, failedTxn);
        } catch (Exception e) {
            log.warn("Payment health unavailable: {}", e.getMessage());
            return new PlatformOpsSnapshot.PaymentHealthStats(0, 0.0, 0, 0);
        }
    }

    public PlatformOpsSnapshot.WebSocketStats getWsStats() {
        // WebSocketSessionTracker tracks per-user; we return total active from Redis/local
        // No global active-connection count exposed by tracker — return 0 as safe default
        return new PlatformOpsSnapshot.WebSocketStats(0, 0);
    }

    public PlatformOpsSnapshot.StorageStats getStorageStats() {
        try {
            File root = new File("/");
            double total = root.getTotalSpace() / 1_073_741_824.0;
            double free  = root.getUsableSpace() / 1_073_741_824.0;
            double used  = total - free;
            double pct   = total > 0 ? (used / total * 100.0) : 0.0;
            return new PlatformOpsSnapshot.StorageStats(
                BigDecimal.valueOf(used).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                BigDecimal.valueOf(pct).setScale(1, RoundingMode.HALF_UP).doubleValue());
        } catch (Exception e) {
            return new PlatformOpsSnapshot.StorageStats(0, 0, 0);
        }
    }

    @Transactional(readOnly = true)
    public List<PlatformOpsSnapshot.SecurityIncidentSummary> getSecurityIncidents(int hours) {
        try {
            Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
            return jdbc.queryForList(
                "SELECT id, category, actor_id, action, created_at FROM audit_journal " +
                "WHERE category::text = 'SECURITY' AND created_at > ? ORDER BY created_at DESC LIMIT 50",
                since).stream().map(row -> new PlatformOpsSnapshot.SecurityIncidentSummary(
                    toLong(row.get("id")),
                    (String) row.get("category"),
                    toLong(row.get("actor_id")),
                    (String) row.get("action"),
                    toInstant(row.get("created_at"))
                )).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Security incidents unavailable: {}", e.getMessage());
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public List<PlatformOpsSnapshot.TenantHealthSummary> getTenantHealth() {
        try {
            return jdbc.queryForList(
                "SELECT c.id, c.name, c.status, " +
                "  (SELECT COUNT(*) FROM app_user u WHERE u.community_id = c.id) AS res_count, " +
                "  (SELECT COUNT(*) FROM helpdesk_ticket ht WHERE ht.community_id = c.id AND ht.status = 'OPEN') AS open_issues, " +
                "  c.created_at " +
                "FROM community c ORDER BY c.id LIMIT 100"
            ).stream().map(row -> new PlatformOpsSnapshot.TenantHealthSummary(
                toLong(row.get("id")),
                (String) row.get("name"),
                (String) row.get("status"),
                toLong(row.get("res_count")),
                toLong(row.get("open_issues")),
                toInstant(row.get("created_at"))
            )).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Tenant health unavailable: {}", e.getMessage());
            return List.of();
        }
    }

    // ─── Metrics Recording ──────────────────────────────────────────────────

    @Transactional
    public void recordMetric(String type, BigDecimal value, String unit) {
        metricsRepo.save(PlatformMetricsHistory.builder()
            .metricType(type).value(value).unit(unit).build());
    }

    @Transactional(readOnly = true)
    public List<PlatformMetricsHistory> getMetricHistory(String type, int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        return metricsRepo.findByMetricTypeAndRecordedAtAfterOrderByRecordedAtDesc(type, since);
    }

    // ─── Alert Management ───────────────────────────────────────────────────

    @Transactional
    public PlatformAlert createAlert(String type, String severity, String title,
                                     String description, Long communityId) {
        return alertRepo.save(PlatformAlert.builder()
            .alertType(type).severity(severity).title(title)
            .description(description).communityId(communityId).build());
    }

    @Transactional
    public void resolveAlert(Long alertId, Long resolvedBy) {
        alertRepo.findById(alertId).ifPresent(a -> {
            a.setResolvedAt(Instant.now());
            a.setResolvedBy(resolvedBy);
            alertRepo.save(a);
        });
    }

    @Transactional(readOnly = true)
    public List<PlatformAlert> getOpenAlertsEntities() {
        return alertRepo.findByResolvedAtIsNullOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<PlatformOpsSnapshot.AlertDto> getOpenAlerts() {
        return getOpenAlertsEntities().stream().map(this::toAlertDto).collect(Collectors.toList());
    }

    // ─── Deploy Events ──────────────────────────────────────────────────────

    @Transactional
    public PlatformDeployEvent recordDeploy(String service, String version, String env,
                                            String status, String commitSha) {
        var event = deployRepo.save(PlatformDeployEvent.builder()
            .serviceName(service).version(version).environment(env)
            .status(status).commitSha(commitSha)
            .finishedAt("SUCCEEDED".equals(status) || "FAILED".equals(status) ? Instant.now() : null)
            .build());
        log.info("Deploy recorded: service={} version={} status={}", service, version, status);
        return event;
    }

    @Transactional(readOnly = true)
    public List<PlatformOpsSnapshot.DeployEventDto> getRecentDeploys(int limit) {
        return deployRepo.findTop20ByOrderByStartedAtDesc().stream()
            .limit(limit).map(this::toDeployDto).collect(Collectors.toList());
    }

    // ─── Scheduled Tasks ────────────────────────────────────────────────────

    @Scheduled(fixedRate = 60_000)
    public void collectMetrics() {
        try {
            var pool = getDbPool();
            recordMetric("DB_POOL_ACTIVE", BigDecimal.valueOf(pool.activeConnections()), "count");
            recordMetric("DB_POOL_IDLE",   BigDecimal.valueOf(pool.idleConnections()),   "count");

            var storage = getStorageStats();
            recordMetric("STORAGE_GB_USED",  BigDecimal.valueOf(storage.diskUsedGb()),  "GB");
            recordMetric("STORAGE_PCT_USED", BigDecimal.valueOf(storage.diskUsedPct()), "percent");

            // Raise alert if DB pool is > 80% active
            if (pool.maxPoolSize() > 0) {
                double utilisation = (double) pool.activeConnections() / pool.maxPoolSize() * 100.0;
                if (utilisation > 80) {
                    createAlert("DB_POOL_EXHAUSTED", "WARNING",
                        "DB pool utilisation high: " + String.format("%.1f", utilisation) + "%",
                        "Active connections approaching max pool size", null);
                }
            }
            // Raise alert if disk > 85%
            if (storage.diskUsedPct() > 85) {
                createAlert("STORAGE_HIGH", "WARNING",
                    "Disk usage at " + String.format("%.1f", storage.diskUsedPct()) + "%",
                    "Consider cleaning up build artefacts or expanding storage", null);
            }
        } catch (Exception e) {
            log.warn("Metric collection failed: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 30_000)
    public void broadcastSnapshot() {
        try {
            messaging.convertAndSend("/topic/platform/ops", getSnapshot());
        } catch (Exception e) {
            log.debug("Platform ops broadcast failed: {}", e.getMessage());
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long l) return l;
        if (v instanceof Number n) return n.longValue();
        return null;
    }

    private Instant toInstant(Object v) {
        if (v == null) return null;
        if (v instanceof java.sql.Timestamp ts) return ts.toInstant();
        if (v instanceof java.time.OffsetDateTime odt) return odt.toInstant();
        return null;
    }

    private PlatformOpsSnapshot.AlertDto toAlertDto(PlatformAlert a) {
        return new PlatformOpsSnapshot.AlertDto(a.getId(), a.getAlertType(), a.getSeverity(),
            a.getTitle(), a.getDescription(), a.getCommunityId(),
            a.getMetricValue(), a.getThresholdValue(), a.getCreatedAt());
    }

    private PlatformOpsSnapshot.DeployEventDto toDeployDto(PlatformDeployEvent d) {
        return new PlatformOpsSnapshot.DeployEventDto(d.getId(), d.getServiceName(), d.getVersion(),
            d.getEnvironment(), d.getStatus(), d.getCommitSha(), d.getDeployedBy(),
            d.getStartedAt(), d.getFinishedAt());
    }
}
