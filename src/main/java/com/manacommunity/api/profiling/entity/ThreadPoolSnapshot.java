package com.manacommunity.api.profiling.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "thread_pool_snapshots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ThreadPoolSnapshot extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serviceId;
    private String serviceName;
    private String poolName;

    @Enumerated(EnumType.STRING)
    private PoolType poolType;

    private int corePoolSize;
    private int maxPoolSize;
    private int activeThreads;
    private int queuedTasks;
    private int queueCapacity;
    private long completedTasks;
    private long rejectedTasks;

    private double cpuUsagePercent;
    private long avgTaskDurationMs;
    private long p99TaskDurationMs;

    @Enumerated(EnumType.STRING)
    private PoolHealth health;

    private boolean virtualThreadsEnabled;
    private int virtualThreadCount;

    private LocalDateTime snapshotTimestamp;
    private String threadDumpSnippet;

    public enum PoolType { TOMCAT_HTTP, SCHEDULED, FORK_JOIN, VIRTUAL_THREAD, CUSTOM, WEBFLUX_EVENT_LOOP }
    public enum PoolHealth { HEALTHY, WARNING, CRITICAL, STARVED }
}
