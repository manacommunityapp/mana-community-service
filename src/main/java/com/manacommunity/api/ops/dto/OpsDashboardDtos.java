package com.manacommunity.api.ops.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

public class OpsDashboardDtos {

    public enum HealthStatus {
        UP, DEGRADED, DOWN, UNKNOWN
    }

    public enum AlertSeverity {
        CRITICAL, WARNING, INFO
    }

    public enum QueueStatus {
        HEALTHY, WARNING, CRITICAL
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceHealthDto {
        private String id;
        private String name;
        private String category; // CORE, INFRASTRUCTURE, DOMAIN, AI_VISION
        private HealthStatus status;
        private int port;
        private String endpoint;
        private long latencyMs;
        private String version;
        private String gitCommit;
        private double errorRatePercent;
        private long uptimeSeconds;
        private String details;
        private Instant lastChecked;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedRequestDto {
        private String id;
        private Instant timestamp;
        private String method;
        private String path;
        private int statusCode;
        private long latencyMs;
        private String correlationId;
        private String errorType;
        private String errorMessage;
        private String clientIp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueueLagDto {
        private String topic;
        private String consumerGroup;
        private int partitions;
        private long totalLag;
        private double consumptionRatePerSec;
        private QueueStatus status;
        private String targetService;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MigrationStatusDto {
        private String schemaName;
        private int totalChangeSets;
        private int executedChangeSets;
        private int pendingChangeSets;
        private String latestChangeSetId;
        private String latestAuthor;
        private Instant lastExecutedAt;
        private String status; // UP_TO_DATE, PENDING, FAILED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncidentAlertDto {
        private String id;
        private AlertSeverity severity;
        private String title;
        private String message;
        private String sourceService;
        private Instant triggeredAt;
        private boolean acknowledged;
        private String acknowledgedBy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemClusterMetricsDto {
        private double cpuLoadPercent;
        private long totalMemoryMb;
        private long usedMemoryMb;
        private double memoryPercent;
        private long totalDiskGb;
        private long usedDiskGb;
        private double diskPercent;
        private long jvmUsedMemoryMb;
        private long jvmMaxMemoryMb;
        private double jvmPercent;
        private int activeThreads;
        private long uptimeSeconds;
        private long totalRequestsTracked;
        private long totalFailedRequests;
        private double overallErrorRate;
    }

    // ── Extended Platform Telemetry DTOs ─────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedNotificationSummaryDto {
        private long totalFailed;
        private long failedLast1h;
        private List<ChannelFailureDto> byChannel;
        private Instant lastFailureAt;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ChannelFailureDto {
            private String channel; // PUSH, EMAIL, SMS, WHATSAPP
            private long failed;
            private long total;
            private double failureRate;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebSocketHealthDto {
        private int activeConnections;
        private int peakConnectionsLast1h;
        private int disconnectsLast1h;
        private double messageErrorRate;
        private long avgSessionDurationSeconds;
        private HealthStatus status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BackgroundJobStatusDto {
        private String jobName;
        private String cronExpression;
        private String status; // HEALTHY, FAILING, STALLED, DISABLED
        private Instant lastRunAt;
        private Instant lastSuccessAt;
        private Instant lastFailureAt;
        private int failureCountLast24h;
        private long avgDurationMs;
        private Instant nextScheduledAt;
        private String lastErrorMessage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CicdPipelineStatusDto {
        private String pipeline;
        private String status; // SUCCESS, RUNNING, FAILED, PENDING, CANCELLED
        private String branch;
        private String commitSha;
        private String triggeredBy;
        private Instant startedAt;
        private Instant finishedAt;
        private Long durationSeconds;
        private String deployedEnvironment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentGatewayHealthDto {
        private String gateway;
        private HealthStatus status;
        private long verificationDelayP95Ms;
        private double successRate;
        private long failureCount;
        private long avgResponseMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantHealthSummaryDto {
        private int totalTenants;
        private int healthyTenants;
        private int degradedTenants;
        private int offlineTenants;
        private int pendingOnboarding;
        private int onboardingCompletedLast7Days;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseConnectionPoolDto {
        private String poolName;
        private int activeConnections;
        private int idleConnections;
        private int maxPoolSize;
        private int pendingAcquire;
        private long avgAcquireTimeMs;
        private String status; // HEALTHY, WARNING, CRITICAL
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PlatformOpsOverviewDto {
        private Instant generatedAt;
        private HealthStatus overallStatus;
        private long uptimeSeconds;
        private int activeWebsockets;
        private long failedRequestsTotal;
        private SystemClusterMetricsDto clusterMetrics;
        private List<ServiceHealthDto> services;
        private List<FailedRequestDto> recentFailures;
        private List<QueueLagDto> queueLags;
        private List<MigrationStatusDto> migrations;
        private List<IncidentAlertDto> activeIncidents;

        // Extended Telemetry Sections
        private FailedNotificationSummaryDto notifications;
        private WebSocketHealthDto webSocketHealth;
        private List<BackgroundJobStatusDto> backgroundJobs;
        private List<CicdPipelineStatusDto> cicdPipelines;
        private List<PaymentGatewayHealthDto> paymentGateways;
        private TenantHealthSummaryDto tenantHealth;
        private List<DatabaseConnectionPoolDto> databasePools;
    }
}