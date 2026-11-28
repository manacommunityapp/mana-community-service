package com.manacommunity.api.release.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "tenant_feature_flags")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TenantFeatureFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flag_key", nullable = false, unique = true, length = 100)
    private String flagKey;

    @Column(length = 300)
    private String description;

    @Builder.Default
    @Column(name = "ring_stage", nullable = false, length = 32)
    private String ringStage = "INTERNAL_PILOT"; // INTERNAL_PILOT, RING_1_SMALL_1PCT, RING_2_MEDIUM_10PCT, RING_3_MEGA_100PCT

    @Builder.Default
    @Column(name = "rollout_bps", nullable = false)
    private int rolloutBps = 0; // 0..10000 basis points (100 = 1%)

    @Builder.Default
    @Column(name = "kill_switch", nullable = false)
    private boolean killSwitch = false;

    @Builder.Default
    @Column(name = "shadow_mode", nullable = false)
    private boolean shadowMode = false;

    @Builder.Default
    @Column(name = "max_p99_latency_ms", nullable = false)
    private int maxP99LatencyMs = 300;

    @Builder.Default
    @Column(name = "max_error_rate_bps", nullable = false)
    private int maxErrorRateBps = 50; // 0.50%

    @Builder.Default
    @Column(name = "block_rush_hours", nullable = false)
    private boolean blockRushHours = true;

    @Column(name = "rollback_reason", length = 255)
    private String rollbackReason;

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
