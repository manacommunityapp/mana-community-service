package com.manacommunity.api.forensics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "ledger_checkpoints", schema = "forensics")
@Getter
@Setter
public class LedgerCheckpointEntity {

    @Id
    @Column(name = "checkpoint_id", nullable = false)
    private UUID checkpointId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "flat_id", nullable = false)
    private Long flatId;

    @Column(name = "unit_code", nullable = false, length = 40)
    private String unitCode;

    @Column(name = "checkpoint_date", nullable = false)
    private LocalDate checkpointDate;

    @Column(name = "recorded_cutoff", nullable = false)
    private Instant recordedCutoff;

    @Column(name = "stream_version", nullable = false)
    private Long streamVersion;

    @Column(name = "balance_paise", nullable = false)
    private Long balancePaise;

    @Column(name = "state_hash", nullable = false, length = 64)
    private String stateHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
