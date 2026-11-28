package com.manacommunity.api.payments.reconciliation.service;

import com.manacommunity.api.payments.reconciliation.model.BankSettlementRecord;
import com.manacommunity.api.payments.reconciliation.model.ReconciliationJournalEntry;
import com.manacommunity.api.payments.reconciliation.model.ReconciliationJournalEntry.JournalLine;
import com.manacommunity.api.payments.reconciliation.model.ReconciliationReport;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-throughput automated T+1 Bank Statement & Settlement File Reconciliation Engine.
 * Matches bank settlement UTRs against internal payment intents, identifies orphan credits,
 * flags MDR fee discrepancies, and generates balanced double-entry accounting journal entries.
 */
@Service
@Slf4j
public class TPlusOneBankReconciliationEngine {

    // Chart of Accounts (COA)
    public static final String COA_BANK_OPERATING = "1001"; // Asset: Main Society Bank Account
    public static final String COA_GATEWAY_CLEARING = "1010"; // Asset: Gateway In-Transit Clearing
    public static final String COA_INPUT_GST_CREDIT = "1300"; // Asset: Tax input credit on bank charges
    public static final String COA_ORPHAN_SUSPENSE = "2900"; // Liability: Unidentified customer credits
    public static final String COA_MDR_EXPENSE = "5100"; // Operating Expense: Gateway MDR charges

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentIntent {
        private String paymentId;
        private String utr;
        private String invoiceId;
        private String flatNumber;
        private BigDecimal expectedGrossAmount;
        private String status; // "CAPTURED", "SETTLED"
    }

    private final Map<String, PaymentIntent> intentsByUtr = new ConcurrentHashMap<>();
    private final Map<String, PaymentIntent> intentsByPaymentId = new ConcurrentHashMap<>();

    private static final Pattern FLAT_PATTERN = Pattern.compile("(?i)\\b(?:flat|unit|apt)\\s*[-:#]?\\s*([A-Z]?\\s*\\d{1,4}[A-Z]?)");

    public void registerPaymentIntent(PaymentIntent intent) {
        if (intent.getUtr() != null && !intent.getUtr().isBlank()) {
            intentsByUtr.put(intent.getUtr().trim(), intent);
        }
        if (intent.getPaymentId() != null && !intent.getPaymentId().isBlank()) {
            intentsByPaymentId.put(intent.getPaymentId().trim(), intent);
        }
    }

    /**
     * Ingests and reconciles a batch of bank settlement records (e.g., 50,000 UTR rows).
     * @param records List of bank settlement records from MT940 / CSV file.
     * @param contractualMdrRate Expected maximum MDR fee percentage (e.g., 0.0015 for 0.15%).
     * @return Detailed reconciliation report with audit trail and journal entries.
     */
    public ReconciliationReport reconcileSettlementBatch(
            String batchId,
            List<BankSettlementRecord> records,
            BigDecimal contractualMdrRate) {

        int matched = 0;
        int orphans = 0;
        int feeVariances = 0;

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalMdr = BigDecimal.ZERO;
        BigDecimal totalGst = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        List<BankSettlementRecord> orphanList = new ArrayList<>();
        List<ReconciliationJournalEntry> journalEntries = new ArrayList<>();
        List<String> auditLogs = new ArrayList<>();

        for (BankSettlementRecord rec : records) {
            totalGross = totalGross.add(rec.getGrossAmount());
            totalMdr = totalMdr.add(rec.getMdrFee());
            totalGst = totalGst.add(rec.getGstOnMdr());
            totalNet = totalNet.add(rec.getNetSettledAmount());

            // 1. Math consistency check: Net + MDR + GST == Gross
            BigDecimal calculatedGross = rec.getNetSettledAmount().add(rec.getMdrFee()).add(rec.getGstOnMdr());
            if (calculatedGross.compareTo(rec.getGrossAmount()) != 0) {
                auditLogs.add(String.format("Arithmetic mismatch in record UTR [%s]: Net %s + MDR %s + GST %s != Gross %s",
                        rec.getUtr(), rec.getNetSettledAmount(), rec.getMdrFee(), rec.getGstOnMdr(), rec.getGrossAmount()));
            }

            // 2. Three-way matching (by UTR or Gateway Payment ID)
            PaymentIntent matchedIntent = null;
            if (rec.getUtr() != null) {
                matchedIntent = intentsByUtr.get(rec.getUtr().trim());
            }
            if (matchedIntent == null && rec.getGatewayPaymentId() != null) {
                matchedIntent = intentsByPaymentId.get(rec.getGatewayPaymentId().trim());
            }

            if (matchedIntent != null) {
                matched++;
                matchedIntent.setStatus("SETTLED");

                // Check contractual MDR variance
                if (contractualMdrRate != null && contractualMdrRate.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal expectedMaxMdr = rec.getGrossAmount().multiply(contractualMdrRate)
                            .setScale(2, RoundingMode.HALF_UP);
                    if (rec.getMdrFee().compareTo(expectedMaxMdr) > 0) {
                        feeVariances++;
                        auditLogs.add(String.format("MDR fee variance detected for UTR [%s]: Bank charged ₹%s, contractual ceiling is ₹%s",
                                rec.getUtr(), rec.getMdrFee(), expectedMaxMdr));
                    }
                }

                // Generate Balanced Double-Entry Journal Entry:
                // Debit:  1001 Bank Operating Account (Net Settled)
                // Debit:  5100 Gateway Processing & MDR Expense (MDR Fee)
                // Debit:  1300 Input GST Tax Credit (GST on MDR)
                // Credit: 1010 Payment Gateway Clearing Account (Gross Amount)
                ReconciliationJournalEntry je = ReconciliationJournalEntry.builder()
                        .journalEntryId("JE-REC-" + UUID.randomUUID())
                        .transactionReference(rec.getUtr())
                        .entryType("SETTLEMENT_RECONCILIATION")
                        .postedAt(Instant.now())
                        .narration(String.format("T+1 Settlement for UTR %s, Inv %s (Net: ₹%s, MDR: ₹%s, GST: ₹%s)",
                                rec.getUtr(), matchedIntent.getInvoiceId(), rec.getNetSettledAmount(), rec.getMdrFee(), rec.getGstOnMdr()))
                        .lines(List.of(
                                JournalLine.builder()
                                        .accountCode(COA_BANK_OPERATING)
                                        .accountName("Society Operating Bank Account")
                                        .debitAmount(rec.getNetSettledAmount())
                                        .creditAmount(BigDecimal.ZERO)
                                        .lineNarration("Net funds deposited into bank account")
                                        .build(),
                                JournalLine.builder()
                                        .accountCode(COA_MDR_EXPENSE)
                                        .accountName("Payment Gateway MDR Fee")
                                        .debitAmount(rec.getMdrFee())
                                        .creditAmount(BigDecimal.ZERO)
                                        .lineNarration("Gateway transaction processing charge")
                                        .build(),
                                JournalLine.builder()
                                        .accountCode(COA_INPUT_GST_CREDIT)
                                        .accountName("Input GST Credit on Bank Charges")
                                        .debitAmount(rec.getGstOnMdr())
                                        .creditAmount(BigDecimal.ZERO)
                                        .lineNarration("18% GST on MDR fees")
                                        .build(),
                                JournalLine.builder()
                                        .accountCode(COA_GATEWAY_CLEARING)
                                        .accountName("Payment Gateway Clearing Account")
                                        .debitAmount(BigDecimal.ZERO)
                                        .creditAmount(rec.getGrossAmount())
                                        .lineNarration("Clear in-transit gross payment for invoice " + matchedIntent.getInvoiceId())
                                        .build()
                        ))
                        .build();

                journalEntries.add(je);

            } else {
                // 3. Orphan Credit Detected!
                orphans++;
                orphanList.add(rec);

                // Try heuristic fuzzy flat extraction from narration
                String detectedFlat = extractPotentialFlat(rec.getNarration());
                String orphanNote = detectedFlat != null ?
                        String.format("Direct bank credit without payment intent (Potential Flat: %s)", detectedFlat) :
                        "Direct bank credit with unknown payee / intent";

                auditLogs.add(String.format("ORPHAN CREDIT detected: UTR [%s], Amount ₹%s, Narration [%s]. %s",
                        rec.getUtr(), rec.getGrossAmount(), rec.getNarration(), orphanNote));

                // Generate Suspense Journal Entry:
                // Debit:  1001 Bank Operating Account (Funds in bank)
                // Credit: 2900 Unreconciled Bank Suspense Account (Liability pending identification)
                ReconciliationJournalEntry suspenseJe = ReconciliationJournalEntry.builder()
                        .journalEntryId("JE-SUSP-" + UUID.randomUUID())
                        .transactionReference(rec.getUtr())
                        .entryType("ORPHAN_SUSPENSE")
                        .postedAt(Instant.now())
                        .narration("Orphan credit pending identification: " + rec.getNarration())
                        .lines(List.of(
                                JournalLine.builder()
                                        .accountCode(COA_BANK_OPERATING)
                                        .accountName("Society Operating Bank Account")
                                        .debitAmount(rec.getGrossAmount())
                                        .creditAmount(BigDecimal.ZERO)
                                        .lineNarration("Unidentified credit landed in bank")
                                        .build(),
                                JournalLine.builder()
                                        .accountCode(COA_ORPHAN_SUSPENSE)
                                        .accountName("Unreconciled Bank Suspense Liability")
                                        .debitAmount(BigDecimal.ZERO)
                                        .creditAmount(rec.getGrossAmount())
                                        .lineNarration("Temporary liability until resident identifies transaction")
                                        .build()
                        ))
                        .build();

                journalEntries.add(suspenseJe);
            }
        }

        log.info("Batch [{}] reconciled: {} total records, {} matched, {} orphans, {} fee variances. Net settled: ₹{}",
                batchId, records.size(), matched, orphans, feeVariances, totalNet);

        return ReconciliationReport.builder()
                .batchId(batchId)
                .totalRecordsParsed(records.size())
                .matchedCount(matched)
                .orphanCount(orphans)
                .feeVarianceCount(feeVariances)
                .totalGrossSettled(totalGross)
                .totalMdrDeducted(totalMdr)
                .totalGstDeducted(totalGst)
                .totalNetCreditedToBank(totalNet)
                .orphanCredits(orphanList)
                .generatedJournalEntries(journalEntries)
                .auditLogs(auditLogs)
                .build();
    }

    private String extractPotentialFlat(String narration) {
        if (narration == null) return null;
        Matcher m = FLAT_PATTERN.matcher(narration);
        if (m.find()) {
            return m.group(1).replaceAll("\\s+", "").toUpperCase();
        }
        return null;
    }

    public void clear() {
        intentsByUtr.clear();
        intentsByPaymentId.clear();
    }
}
