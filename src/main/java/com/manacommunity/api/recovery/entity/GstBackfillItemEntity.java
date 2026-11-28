package com.manacommunity.api.recovery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gst_backfill_items", schema = "recovery")
@Getter
@Setter
public class GstBackfillItemEntity {

    @Id
    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "flat_id", nullable = false)
    private Long flatId;

    @Column(name = "unit_code", nullable = false, length = 40)
    private String unitCode;

    @Column(name = "original_voucher_id", nullable = false)
    private UUID originalVoucherId;

    @Column(name = "billed_gst_paise", nullable = false)
    private Long billedGstPaise;

    @Column(name = "expected_gst_paise", nullable = false)
    private Long expectedGstPaise;

    @Column(name = "delta_paise", nullable = false)
    private Long deltaPaise;

    @Column(name = "debit_note_number", nullable = false, length = 80)
    private String debitNoteNumber;

    @Column(name = "idempotency_key", nullable = false, length = 140)
    private String idempotencyKey;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "DRY_RUN";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
