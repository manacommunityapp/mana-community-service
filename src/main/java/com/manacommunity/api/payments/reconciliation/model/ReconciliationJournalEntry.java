package com.manacommunity.api.payments.reconciliation.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Penny-exact double-entry accounting journal entry.
 * Guarantees that Total Debits == Total Credits.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationJournalEntry {

    private String journalEntryId;
    private String transactionReference;
    private String entryType; // "SETTLEMENT_RECONCILIATION", "ADVANCE_WALLET_CREDIT", "ORPHAN_SUSPENSE"
    private Instant postedAt;
    private String narration;

    @Builder.Default
    private List<JournalLine> lines = new ArrayList<>();

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JournalLine {
        private String accountCode;
        private String accountName;
        private BigDecimal debitAmount;
        private BigDecimal creditAmount;
        private String lineNarration;
    }

    public boolean isBalanced() {
        BigDecimal totalDebits = lines.stream()
                .map(JournalLine::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = lines.stream()
                .map(JournalLine::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalDebits.compareTo(totalCredits) == 0;
    }

    public BigDecimal getTotalDebits() {
        return lines.stream()
                .map(JournalLine::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalCredits() {
        return lines.stream()
                .map(JournalLine::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
