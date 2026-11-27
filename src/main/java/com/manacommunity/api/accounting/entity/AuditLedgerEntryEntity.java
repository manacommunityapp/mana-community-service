package com.manacommunity.api.accounting.entity;

import com.manacommunity.api.accounting.dto.AccountingDtos.LedgerLineDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "audit_ledger_entries", schema = "accounting")
@Getter
@Setter
@NoArgsConstructor
public class AuditLedgerEntryEntity {

    @Id
    @Column(name = "entry_id", nullable = false)
    private UUID entryId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "seq_no", nullable = false)
    private long seqNo;

    @Column(name = "voucher_id", nullable = false)
    private UUID voucherId;

    @Column(name = "voucher_number", nullable = false, length = 100)
    private String voucherNumber;

    @Column(name = "voucher_type", nullable = false, length = 30)
    private String voucherType;

    @Column(name = "posting_date", nullable = false)
    private LocalDate postingDate;

    @Column(name = "source_system", nullable = false, length = 20)
    private String sourceSystem;

    @Column(name = "reverses_entry_id")
    private UUID reversesEntryId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "canonical_lines_json", columnDefinition = "jsonb", nullable = false)
    private List<LedgerLineDto> canonicalLinesJson;

    @Column(name = "tax_snapshot_sha256", nullable = false, length = 64)
    private String taxSnapshotSha256;

    @Column(name = "prev_entry_hash", nullable = false, length = 64)
    private String prevEntryHash;

    @Column(name = "entry_hash", nullable = false, length = 64)
    private String entryHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
