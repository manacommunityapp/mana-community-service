package com.manacommunity.api.profiling.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_loop_snapshots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventLoopSnapshot extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serviceId;
    private String serviceName;
    private String nodeVersion;

    private double eventLoopLagMs;
    private double eventLoopLagP99Ms;
    private long eventLoopUtilizationPercent;

    private long activeHandles;
    private long activeRequests;

    private long heapUsedMb;
    private long heapTotalMb;
    private long externalMb;
    private long arrayBuffersMb;
    private long rssMemoryMb;

    @Enumerated(EnumType.STRING)
    private BlockingCause blockingCause;

    private long blockingDurationMs;
    private String blockingStackTrace;

    private int workerThreadsActive;
    private int workerThreadsIdle;
    private long jobQueueDepth;

    @Enumerated(EnumType.STRING)
    private LoopHealth health;

    private LocalDateTime snapshotTimestamp;

    public enum BlockingCause { NONE, PDF_GENERATION, CSV_EXPORT, JSON_PARSE_LARGE, CRYPTO_OPERATION, REGEX_BACKTRACK, SYNCHRONOUS_FILE_IO }
    public enum LoopHealth { HEALTHY, LAGGING, BLOCKED, CRITICAL }
}
