package com.manacommunity.api.release.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "chaos_gameday_experiments")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ChaosGamedayExperiment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "experiment_code", nullable = false, unique = true, length = 64)
    private String experimentCode;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "fault_type", nullable = false, length = 32)
    private String faultType; // REDIS_EVICTION, KAFKA_LAG, FCM_THROTTLE

    @Builder.Default
    @Column(nullable = false, length = 24)
    private String status = "RUNNING"; // RUNNING, PASSED, AUTO_ABORTED

    @Builder.Default
    @Column(name = "max_duration_sec", nullable = false)
    private int maxDurationSec = 600;

    @Builder.Default
    @Column(name = "sos_probe_failed", nullable = false)
    private boolean sosProbeFailed = false;

    @Builder.Default
    @Column(name = "gate_degraded_ok", nullable = false)
    private boolean gateDegradedOk = true;

    @Column(name = "abort_reason", length = 255)
    private String abortReason;

    @Builder.Default
    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "ended_at")
    private Instant endedAt;
}
