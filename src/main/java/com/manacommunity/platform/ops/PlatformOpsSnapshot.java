package com.manacommunity.platform.ops;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PlatformOpsSnapshot(
    Instant capturedAt,
    ApiHealthMetrics apiHealth,
    BackgroundJobStats jobStats,
    DbPoolMetrics dbPool,
    NotificationStats notificationStats,
    PaymentHealthStats paymentHealth,
    WebSocketStats wsStats,
    StorageStats storageStats,
    List<DeployEventDto> recentDeploys,
    List<AlertDto> openAlerts,
    List<SecurityIncidentSummary> securityIncidents,
    List<TenantHealthSummary> tenantHealth
) {

    public record ApiHealthMetrics(
        String overallStatus,
        long avgLatencyMs,
        long p95LatencyMs,
        long p99LatencyMs,
        double errorRatePct,
        long requestsPerMin
    ) {
        public static ApiHealthMetrics healthy() {
            return new ApiHealthMetrics("HEALTHY", 0, 0, 0, 0.0, 0);
        }
    }

    public record BackgroundJobStats(
        int scheduledJobCount,
        long failedLast24h,
        long pendingOutbox,
        long failedOutbox
    ) {}

    public record DbPoolMetrics(
        int activeConnections,
        int idleConnections,
        int maxPoolSize,
        int waitingThreads,
        double avgAcquireMs
    ) {}

    public record NotificationStats(
        long sentLast24h,
        long failedLast24h,
        double failureRatePct,
        Map<String, Long> failedByChannel
    ) {}

    public record PaymentHealthStats(
        long pendingVerifications,
        double avgVerificationDelayMin,
        long stalledPayments,
        long failedLast24h
    ) {}

    public record WebSocketStats(
        int activeConnections,
        int messagesPerMin
    ) {}

    public record StorageStats(
        double diskUsedGb,
        double diskTotalGb,
        double diskUsedPct
    ) {}

    public record SecurityIncidentSummary(
        Long incidentId,
        String category,
        Long actorId,
        String description,
        Instant occurredAt
    ) {}

    public record TenantHealthSummary(
        Long communityId,
        String communityName,
        String status,
        long residentCount,
        long openIssues,
        Instant lastActiveAt
    ) {}

    public record DeployEventDto(
        Long id,
        String serviceName,
        String version,
        String environment,
        String status,
        String commitSha,
        String deployedBy,
        Instant startedAt,
        Instant finishedAt
    ) {}

    public record AlertDto(
        Long id,
        String alertType,
        String severity,
        String title,
        String description,
        Long communityId,
        BigDecimal metricValue,
        BigDecimal thresholdValue,
        Instant createdAt
    ) {}
}
