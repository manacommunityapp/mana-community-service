package com.manacommunity.api.forensics.entity;

import com.manacommunity.api.forensics.dto.ForensicsDtos.LedgerEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bitemporal_ledger_events", schema = "forensics")
@Getter
@Setter
public class BitemporalLedgerEventEntity {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "flat_id", nullable = false)
    private Long flatId;

    @Column(name = "unit_code", nullable = false, length = 40)
    private String unitCode;

    @Column(name = "stream_version", nullable = false)
    private Long streamVersion;

    @Column(name = "global_seq", nullable = false)
    private Long globalSeq;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 60)
    private LedgerEventType eventType;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "signed_amount_paise", nullable = false)
    private Long signedAmountPaise;

    @Column(name = "causes_event_id")
    private UUID causesEventId;

    @Column(name = "reason_code", length = 160)
    private String reasonCode;

    @Column(name = "actor_name", nullable = false, length = 120)
    private String actorName;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "lines_json", nullable = false, columnDefinition = "TEXT")
    private String linesJson;

    @Column(name = "prev_event_hash", nullable = false, length = 64)
    private String prevEventHash;

    @Column(name = "event_hash", nullable = false, length = 64)
    private String eventHash;
}
