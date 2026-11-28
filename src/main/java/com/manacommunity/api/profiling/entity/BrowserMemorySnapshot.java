package com.manacommunity.api.profiling.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "browser_memory_snapshots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BrowserMemorySnapshot extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String portalName;
    private String userId;
    private String browserVersion;
    private String sessionId;

    private long jsHeapUsedMb;
    private long jsHeapTotalMb;
    private long jsHeapLimitMb;
    private long domNodeCount;
    private long detachedDomNodes;
    private long eventListenerCount;

    private long websocketConnections;
    private long websocketMessagesQueued;
    private boolean websocketListenersCleaned;

    private long reactQueryCacheEntries;
    private long reactQueryStaleCacheEntries;
    private long reactComponentMountCount;
    private long reactComponentUnmountMissing;

    @Enumerated(EnumType.STRING)
    private LeakSource primaryLeakSource;

    private long sessionDurationMinutes;
    private long memoryGrowthRateMbPerHour;

    @Enumerated(EnumType.STRING)
    private MemoryHealth health;

    private LocalDateTime snapshotTimestamp;
    private String heapSnapshotSummary;

    public enum LeakSource { DETACHED_DOM, UNCLEANED_WEBSOCKET, UNBOUNDED_QUERY_CACHE, EVENT_LISTENER_ACCUMULATION, CLOSURE_RETENTION, TIMER_ACCUMULATION, NONE }
    public enum MemoryHealth { HEALTHY, GROWING, LEAKING, CRITICAL }
}
