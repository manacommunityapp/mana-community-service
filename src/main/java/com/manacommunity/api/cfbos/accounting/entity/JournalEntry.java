package com.manacommunity.api.cfbos.accounting.entity;

import com.manacommunity.api.cfbos.accounting.enums.JournalEntryStatus;
import com.manacommunity.api.cfbos.accounting.enums.JournalEntryType;
import com.manacommunity.api.cfbos.shared.enums.SourceModule;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cfbos_journal_entry")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JournalEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "entry_number", nullable = false, unique = true, length = 30)
    private String entryNumber;
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    private FiscalYear fiscalYear;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accounting_period_id", nullable = false)
    private AccountingPeriod accountingPeriod;
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20) @Builder.Default
    private JournalEntryType entryType = JournalEntryType.STANDARD;
    @Enumerated(EnumType.STRING)
    @Column(name = "source_module", length = 30)
    private SourceModule sourceModule;
    @Column(name = "source_document_type", length = 50)
    private String sourceDocumentType;
    @Column(name = "source_document_id")
    private Long sourceDocumentId;
    @Column(nullable = false)
    private String narration;
    @Column(name = "total_debit", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalDebit;
    @Column(name = "total_credit", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalCredit;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) @Builder.Default
    private JournalEntryStatus status = JournalEntryStatus.DRAFT;
    private Long postedBy;
    private LocalDateTime postedAt;
    private Long reversedBy;
    private LocalDateTime reversedAt;
    @Column(name = "reversal_of_id")
    private Long reversalOfId;
    @Column(nullable = false) @Builder.Default
    private Integer version = 0;
    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<JournalLine> lines = new ArrayList<>();
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
