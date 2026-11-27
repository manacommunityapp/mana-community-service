package com.manacommunity.api.accounting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tax_calculation_snapshots", schema = "accounting")
@Getter
@Setter
@NoArgsConstructor
public class TaxCalculationSnapshotEntity {

    @Id
    @Column(name = "snapshot_id", nullable = false)
    private UUID snapshotId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "voucher_id", nullable = false)
    private UUID voucherId;

    @Column(name = "voucher_number", nullable = false, length = 100)
    private String voucherNumber;

    @Column(name = "rule_version_id", nullable = false)
    private UUID ruleVersionId;

    @Column(name = "rule_family", nullable = false, length = 30)
    private String ruleFamily;

    @Column(name = "fy_code", nullable = false, length = 10)
    private String fyCode;

    @Column(name = "supply_date", nullable = false)
    private LocalDate supplyDate;

    @Column(name = "base_amount_paise", nullable = false)
    private long baseAmountPaise;

    @Column(name = "computed_tax_paise", nullable = false)
    private long computedTaxPaise;

    @Column(name = "cgst_paise", nullable = false)
    private long cgstPaise = 0L;

    @Column(name = "sgst_paise", nullable = false)
    private long sgstPaise = 0L;

    @Column(name = "tds_paise", nullable = false)
    private long tdsPaise = 0L;

    @Column(name = "applied_rate_bps", nullable = false)
    private int appliedRateBps;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "inputs_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> inputsJson;

    @Column(name = "snapshot_sha256", nullable = false, length = 64)
    private String snapshotSha256;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
