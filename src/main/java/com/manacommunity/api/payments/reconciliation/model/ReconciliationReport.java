package com.manacommunity.api.payments.reconciliation.model;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Summary metrics and discrepancies identified during automated bank statement reconciliation.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationReport {

    private String batchId;
    private int totalRecordsParsed;
    private int matchedCount;
    private int orphanCount;
    private int feeVarianceCount;

    private BigDecimal totalGrossSettled;
    private BigDecimal totalMdrDeducted;
    private BigDecimal totalGstDeducted;
    private BigDecimal totalNetCreditedToBank;

    @Builder.Default
    private List<BankSettlementRecord> orphanCredits = new ArrayList<>();

    @Builder.Default
    private List<ReconciliationJournalEntry> generatedJournalEntries = new ArrayList<>();

    @Builder.Default
    private List<String> auditLogs = new ArrayList<>();
}
