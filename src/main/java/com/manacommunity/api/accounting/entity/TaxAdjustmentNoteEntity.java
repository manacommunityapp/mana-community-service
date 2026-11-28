package com.manacommunity.api.accounting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tax_adjustment_notes", schema = "accounting")
@Getter
@Setter
@NoArgsConstructor
public class TaxAdjustmentNoteEntity {

    @Id
    @Column(name = "note_id", nullable = false)
    private UUID noteId;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "original_voucher_id", nullable = false)
    private UUID originalVoucherId;

    @Column(name = "note_type", nullable = false, length = 20)
    private String noteType;

    @Column(name = "note_number", nullable = false, length = 100)
    private String noteNumber;

    @Column(name = "delta_paise", nullable = false)
    private long deltaPaise;

    @Column(name = "rule_version_id", nullable = false)
    private UUID ruleVersionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
