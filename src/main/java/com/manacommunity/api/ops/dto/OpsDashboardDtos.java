package com.manacommunity.api.ops.dto;

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformOpsOverviewDto {
        private Instant generatedAt;
        private HealthStatus overallStatus;
        private SystemClusterMetricsDto clusterMetrics;
        private List<ServiceHealthDto> services;
        private List<FailedRequestDto> recentFailures;
        private List<QueueLagDto> queueLags;
        private List<MigrationStatusDto> migrations;
        private List<IncidentAlertDto> activeIncidents;
    }
}