package com.manacommunity.api.recovery.entity;

import com.manacommunity.api.recovery.dto.RecoveryDtos.BackfillJobStatus;
import com.manacommunity.api.recovery.dto.RecoveryDtos.BackfillSettlementMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gst_backfill_jobs", schema = "recovery")
@Getter
@Setter
public class GstBackfillJobEntity {

    @Id
    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "incident_ref", nullable = false, length = 80)
    private String incidentRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private BackfillJobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_mode", nullable = false, length = 40)
    private BackfillSettlementMode settlementMode = BackfillSettlementMode.ROLL_TO_NEXT_CYCLE;

    @Column(name = "affected_societies", nullable = false)
    private Integer affectedSocieties = 0;

    @Column(name = "affected_flats", nullable = false)
    private Integer affectedFlats = 0;

    @Column(name = "processed_items", nullable = false)
    private Integer processedItems = 0;

    @Column(name = "total_delta_paise", nullable = false)
    private Long totalDeltaPaise = 0L;

    @Column(name = "current_batch_size", nullable = false)
    private Integer currentBatchSize = 100;

    @Column(name = "last_replica_lag_ms", nullable = false)
    private Integer lastReplicaLagMs = 0;

    @Column(name = "sleep_interval_ms", nullable = false)
    private Integer sleepIntervalMs = 20;

    @Column(name = "treasurer_notified_count", nullable = false)
    private Integer treasurerNotifiedCount = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
