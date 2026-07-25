package com.manacommunity.api.cfbos.accounting.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cfbos_journal_line")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JournalLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    @Column(name = "cost_center_id")
    private Long costCenterId;
    @Column(name = "fund_id")
    private Long fundId;
    @Column(name = "debit_amount", precision = 18, scale = 2, nullable = false) @Builder.Default
    private BigDecimal debitAmount = BigDecimal.ZERO;
    @Column(name = "credit_amount", precision = 18, scale = 2, nullable = false) @Builder.Default
    private BigDecimal creditAmount = BigDecimal.ZERO;
    private String narration;
}
