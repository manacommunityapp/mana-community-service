package com.manacommunity.api.profiling.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "gc_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GcEvent extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serviceId;
    private String serviceName;
    private String jvmVersion;

    @Enumerated(EnumType.STRING)
    private GcCollector collector;

    @Enumerated(EnumType.STRING)
    private GcCause cause;

    private long pauseMillis;
    private boolean stopTheWorld;

    private long heapBeforeMb;
    private long heapAfterMb;
    private long heapMaxMb;
    private long edenUsedMb;
    private long survivorUsedMb;
    private long oldGenUsedMb;
    private long metaspaceUsedMb;

    private int gcThreadCount;
    private long allocationRateMbPerSec;
    private long promotionRateMbPerSec;

    private LocalDateTime eventTimestamp;
    private String gcLogSnippet;

    public enum GcCollector { G1GC, ZGC, SHENANDOAH, PARALLEL, SERIAL, CMS }
    public enum GcCause { ALLOCATION_FAILURE, G1_HUMONGOUS_ALLOCATION, METADATA_GC_THRESHOLD, SYSTEM_GC, G1_EVACUATION_PAUSE, GCLOCKER, PROACTIVE }
}
