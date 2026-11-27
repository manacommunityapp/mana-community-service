package com.manacommunity.api.accounting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tax_recalculation_batches", schema = "accounting")
@Getter
@Setter
@NoArgsConstructor
public class TaxRecalculationBatchEntity {

    @Id
    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "fy_code", nullable = false, length = 10)
    private String fyCode;

    @Column(name = "rule_family", nullable = false, length = 30)
    private String ruleFamily;

    @Column(name = "target_rule_version_id", nullable = false)
    private UUID targetRuleVersionId;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(name = "evaluated_count", nullable = false)
    private int evaluatedCount = 0;

    @Column(name = "adjusted_count", nullable = false)
    private int adjustedCount = 0;

    @Column(name = "net_delta_paise", nullable = false)
    private long netDeltaPaise = 0L;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
