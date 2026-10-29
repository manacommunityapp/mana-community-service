package com.manacommunity.api.transaction.pillar;

import com.manacommunity.api.cfbos.accounting.dto.JournalEntryRequest;
import com.manacommunity.api.cfbos.accounting.engine.AccountingEngine;
import com.manacommunity.api.cfbos.accounting.entity.JournalEntry;
import com.manacommunity.api.cfbos.shared.enums.SourceModule;
import com.manacommunity.api.transaction.enums.TransactionPaymentMethod;
import com.manacommunity.api.transaction.model.TransactionIntent;
import com.manacommunity.api.transaction.model.TransactionSplitDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionLedgerPillar {

    private final AccountingEngine accountingEngine;

    @Transactional
    public JournalEntry recordTransactionJournal(TransactionIntent intent, String transactionNumber, TransactionSplitDetail split) {
        LocalDate entryDate = LocalDate.now();
        List<JournalEntryRequest.LineRequest> lines = new ArrayList<>();

        BigDecimal totalAmount = intent.getAmount();

        // 1. DEBIT Side: Source of Funds (Wallet Liability vs Bank Clearing)
        String debitAccountCode = (intent.getPaymentMethod() == TransactionPaymentMethod.WALLET) ? "2100" : "1110";
        String debitNarration = (intent.getPaymentMethod() == TransactionPaymentMethod.WALLET)
                ? "Wallet debit for " + intent.getDomain() + " [" + transactionNumber + "]"
                : "Payment collection via " + intent.getPaymentMethod() + " [" + transactionNumber + "]";

        lines.add(JournalEntryRequest.LineRequest.builder()
                .accountCode(debitAccountCode)
                .debitAmount(totalAmount)
                .creditAmount(BigDecimal.ZERO)
                .narration(debitNarration)
                .build());

        // 2. CREDIT Side: Allocation
        if (intent.isEscrowRequired()) {
            // Escrow holding account
            lines.add(JournalEntryRequest.LineRequest.builder()
                    .accountCode("2150") // Escrow Liability Account
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(totalAmount)
                    .narration("Escrow hold for " + intent.getDomain() + " [" + transactionNumber + "]")
                    .build());
        } else {
            BigDecimal vendorPayout = (split != null && split.getVendorPayout().compareTo(BigDecimal.ZERO) > 0)
                    ? split.getVendorPayout() : BigDecimal.ZERO;
            BigDecimal platformFee = (split != null && split.getPlatformFee().compareTo(BigDecimal.ZERO) > 0)
                    ? split.getPlatformFee() : BigDecimal.ZERO;
            BigDecimal tdsAmount = (split != null && split.getTdsAmount().compareTo(BigDecimal.ZERO) > 0)
                    ? split.getTdsAmount() : BigDecimal.ZERO;
            BigDecimal taxAmount = (split != null && split.getTaxAmount().compareTo(BigDecimal.ZERO) > 0)
                    ? split.getTaxAmount() : BigDecimal.ZERO;

            BigDecimal accountedCredit = vendorPayout.add(platformFee).add(tdsAmount).add(taxAmount);
            BigDecimal remainingRevenue = totalAmount.subtract(accountedCredit);

            if (vendorPayout.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(JournalEntryRequest.LineRequest.builder()
                        .accountCode("2000") // Accounts Payable / Vendor Payable
                        .debitAmount(BigDecimal.ZERO)
                        .creditAmount(vendorPayout)
                        .narration("Vendor payout allocation [" + transactionNumber + "]")
                        .build());
            }

            if (platformFee.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(JournalEntryRequest.LineRequest.builder()
                        .accountCode("4200") // Platform Commission Income
                        .debitAmount(BigDecimal.ZERO)
                        .creditAmount(platformFee)
                        .narration("Platform fee [" + transactionNumber + "]")
                        .build());
            }

            if (tdsAmount.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(JournalEntryRequest.LineRequest.builder()
                        .accountCode("2220") // TDS Payable
                        .debitAmount(BigDecimal.ZERO)
                        .creditAmount(tdsAmount)
                        .narration("TDS deduction [" + transactionNumber + "]")
                        .build());
            }

            if (taxAmount.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(JournalEntryRequest.LineRequest.builder()
                        .accountCode("2200") // Tax / GST Payable
                        .debitAmount(BigDecimal.ZERO)
                        .creditAmount(taxAmount)
                        .narration("GST/Tax collected [" + transactionNumber + "]")
                        .build());
            }

            if (remainingRevenue.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(JournalEntryRequest.LineRequest.builder()
                        .accountCode("4100") // Domain Operating Revenue
                        .debitAmount(BigDecimal.ZERO)
                        .creditAmount(remainingRevenue)
                        .narration("Operating revenue for " + intent.getDomain() + " [" + transactionNumber + "]")
                        .build());
            }
        }

        try {
            JournalEntryRequest request = JournalEntryRequest.builder()
                    .entryDate(entryDate)
                    .sourceModule(SourceModule.PAYMENT)
                    .sourceDocumentType(intent.getDomain().name())
                    .narration("Transaction Core entry for " + intent.getDomain() + " [" + transactionNumber + "]")
                    .lines(lines)
                    .build();

            return accountingEngine.createAndPostJournalEntry(request);
        } catch (Exception e) {
            log.warn("Failed to post journal entry for transaction {}: {}", transactionNumber, e.getMessage());
            return null;
        }
    }

    @Transactional
    public JournalEntry recordEscrowReleaseJournal(String transactionNumber, TransactionSplitDetail split) {
        LocalDate entryDate = LocalDate.now();
        List<JournalEntryRequest.LineRequest> lines = new ArrayList<>();

        BigDecimal totalAmount = split.getGrossAmount();

        // DR Escrow Liability (2150)
        lines.add(JournalEntryRequest.LineRequest.builder()
                .accountCode("2150")
                .debitAmount(totalAmount)
                .creditAmount(BigDecimal.ZERO)
                .narration("Escrow release for [" + transactionNumber + "]")
                .build());

        // CR Allocations
        if (split.getVendorPayout().compareTo(BigDecimal.ZERO) > 0) {
            lines.add(JournalEntryRequest.LineRequest.builder()
                    .accountCode("2000")
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(split.getVendorPayout())
                    .narration("Vendor payout after escrow release [" + transactionNumber + "]")
                    .build());
        }

        if (split.getPlatformFee().compareTo(BigDecimal.ZERO) > 0) {
            lines.add(JournalEntryRequest.LineRequest.builder()
                    .accountCode("4200")
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(split.getPlatformFee())
                    .narration("Platform fee recognized [" + transactionNumber + "]")
                    .build());
        }

        if (split.getTdsAmount().compareTo(BigDecimal.ZERO) > 0) {
            lines.add(JournalEntryRequest.LineRequest.builder()
                    .accountCode("2220")
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(split.getTdsAmount())
                    .narration("TDS payable [" + transactionNumber + "]")
                    .build());
        }

        if (split.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            lines.add(JournalEntryRequest.LineRequest.builder()
                    .accountCode("2200")
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(split.getTaxAmount())
                    .narration("Tax payable [" + transactionNumber + "]")
                    .build());
        }

        try {
            JournalEntryRequest request = JournalEntryRequest.builder()
                    .entryDate(entryDate)
                    .sourceModule(SourceModule.PAYMENT)
                    .sourceDocumentType("ESCROW_RELEASE")
                    .narration("Escrow release journal for [" + transactionNumber + "]")
                    .lines(lines)
                    .build();

            return accountingEngine.createAndPostJournalEntry(request);
        } catch (Exception e) {
            log.warn("Failed to post escrow release journal for transaction {}: {}", transactionNumber, e.getMessage());
            return null;
        }
    }
}
