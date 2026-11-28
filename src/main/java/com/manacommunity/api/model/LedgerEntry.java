package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_entry", indexes = {
    @Index(name = "idx_ledger_entry_journal", columnList = "journal_id"),
    @Index(name = "idx_ledger_entry_account", columnList = "account_id"),
    @Index(name = "idx_ledger_entry_community", columnList = "community_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "journal_id", nullable = false, length = 40)
    private String journalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    @ToString.Exclude
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @ToString.Exclude
    private LedgerAccount account;

    @Column(name = "entry_type", nullable = false, length = 10)
    private String entryType;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(length = 255)
    private String narration;

    @Column(name = "posted_at", nullable = false, updatable = false)
    private LocalDateTime postedAt;

    @Column(name = "posted_by")
    private Long postedBy;

    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    @Column(name = "fiscal_period", nullable = false)
    private Integer fiscalPeriod;

    @Column(name = "is_reversed", nullable = false)
    @Builder.Default
    private Boolean isReversed = false;

    @Column(name = "reversal_journal_id", length = 40)
    private String reversalJournalId;

    @PrePersist
    protected void onCreate() {
        if (postedAt == null) postedAt = LocalDateTime.now();
    }
}
