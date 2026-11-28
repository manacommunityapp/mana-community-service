package com.manacommunity.api.payments.reconciliation.service;

import com.manacommunity.api.payments.reconciliation.model.PaymentWebhookPayload;
import com.manacommunity.api.payments.reconciliation.model.ReconciliationJournalEntry;
import com.manacommunity.api.payments.reconciliation.model.ReconciliationJournalEntry.JournalLine;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles delayed, out-of-order, and duplicate payment gateway webhooks.
 * Detects race conditions when a resident double-pays an invoice,
 * safely redirects excess funds to the Resident Advance Maintenance Wallet,
 * and maintains penny-exact General Ledger double-entry balance.
 */
@Service
@Slf4j
public class DelayedWebhookHandler {

    public enum PaymentDisposition {
        INVOICE_SETTLED,
        CREDITED_TO_ADVANCE_WALLET,
        DUPLICATE_WEBHOOK_ACKNOWLEDGED,
        REJECTED_INVALID_AMOUNT
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InvoiceRecord {
        private String invoiceId;
        private String flatNumber;
        private String residentId;
        private BigDecimal billAmount;
        private BigDecimal paidAmount;
        private boolean paid;
        private String settledPaymentId;
        private String settledPaymentMethod;
        private Instant settledAt;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AdvanceWallet {
        private String flatNumber;
        private String residentId;
        @Builder.Default
        private BigDecimal balance = BigDecimal.ZERO;
        @Builder.Default
        private List<WalletTransaction> history = new ArrayList<>();
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WalletTransaction {
        private String transactionId;
        private BigDecimal amount;
        private String paymentRef;
        private String utr;
        private String narration;
        private Instant timestamp;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WebhookProcessingResult {
        private String paymentId;
        private String invoiceId;
        private String flatNumber;
        private PaymentDisposition disposition;
        private BigDecimal amountProcessed;
        private BigDecimal currentWalletBalance;
        private ReconciliationJournalEntry generatedJournalEntry;
        private String residentNotification;
    }

    // Chart of Accounts (COA)
    public static final String COA_GATEWAY_CLEARING = "1010"; // Asset
    public static final String COA_ACCOUNTS_RECEIVABLE = "1200"; // Asset (Reduced on invoice pay)
    public static final String COA_ADVANCE_MAINTENANCE_LIABILITY = "2100"; // Liability (Prepaid wallet)
    public static final String COA_MAINTENANCE_INCOME = "4001"; // Revenue

    // State registries (backed by DB in production)
    private final Map<String, InvoiceRecord> invoiceStore = new ConcurrentHashMap<>();
    private final Map<String, AdvanceWallet> walletStore = new ConcurrentHashMap<>();
    private final Set<String> processedPaymentIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final List<ReconciliationJournalEntry> postedGeneralLedger = Collections.synchronizedList(new ArrayList<>());

    public void registerInvoice(InvoiceRecord invoice) {
        invoiceStore.put(invoice.getInvoiceId(), invoice);
    }

    public InvoiceRecord getInvoice(String invoiceId) {
        return invoiceStore.get(invoiceId);
    }

    public AdvanceWallet getWallet(String flatNumber) {
        return walletStore.computeIfAbsent(flatNumber, f -> AdvanceWallet.builder()
                .flatNumber(f)
                .balance(BigDecimal.ZERO)
                .build());
    }

    public List<ReconciliationJournalEntry> getPostedJournalEntries() {
        return Collections.unmodifiableList(postedGeneralLedger);
    }

    /**
     * Atomically processes incoming payment gateway webhook.
     */
    public synchronized WebhookProcessingResult processPaymentWebhook(PaymentWebhookPayload webhook) {
        String paymentId = webhook.getPaymentId();
        String invoiceId = webhook.getInvoiceId();
        BigDecimal amount = webhook.getAmount();

        // 1. Idempotency Check (Duplicate webhook retry protection)
        if (processedPaymentIds.contains(paymentId)) {
            log.info("Duplicate webhook delivery detected for paymentId [{}]. Acknowledging immediately.", paymentId);
            return WebhookProcessingResult.builder()
                    .paymentId(paymentId)
                    .invoiceId(invoiceId)
                    .disposition(PaymentDisposition.DUPLICATE_WEBHOOK_ACKNOWLEDGED)
                    .amountProcessed(amount)
                    .build();
        }

        InvoiceRecord invoice = invoiceStore.get(invoiceId);
        if (invoice == null) {
            log.error("Payment webhook received for non-existent invoice [{}]", invoiceId);
            throw new IllegalArgumentException("Invoice not found: " + invoiceId);
        }

        String flatNumber = invoice.getFlatNumber();
        AdvanceWallet wallet = getWallet(flatNumber);

        // 2. Scenario A: Invoice is UNPAID -> Standard settlement
        if (!invoice.isPaid()) {
            invoice.setPaid(true);
            invoice.setPaidAmount(amount);
            invoice.setSettledPaymentId(paymentId);
            invoice.setSettledPaymentMethod(webhook.getPaymentMethod());
            invoice.setSettledAt(Instant.now());
            processedPaymentIds.add(paymentId);

            // Double-entry GL:
            // Debit: 1010 Gateway Clearing (Asset +)
            // Credit: 1200 Accounts Receivable / Maintenance Due (Asset -)
            ReconciliationJournalEntry journalEntry = ReconciliationJournalEntry.builder()
                    .journalEntryId("JE-PAY-" + UUID.randomUUID())
                    .transactionReference(paymentId)
                    .entryType("INVOICE_SETTLEMENT")
                    .postedAt(Instant.now())
                    .narration(String.format("Payment received for Invoice %s, Flat %s via %s (UTR: %s)",
                            invoiceId, flatNumber, webhook.getPaymentMethod(), webhook.getUtr()))
                    .lines(List.of(
                            JournalLine.builder()
                                    .accountCode(COA_GATEWAY_CLEARING)
                                    .accountName("Payment Gateway Clearing")
                                    .debitAmount(amount)
                                    .creditAmount(BigDecimal.ZERO)
                                    .lineNarration("Gateway funds received pending bank settlement")
                                    .build(),
                            JournalLine.builder()
                                    .accountCode(COA_ACCOUNTS_RECEIVABLE)
                                    .accountName("Maintenance Accounts Receivable")
                                    .debitAmount(BigDecimal.ZERO)
                                    .creditAmount(amount)
                                    .lineNarration("Settle outstanding maintenance dues for Flat " + flatNumber)
                                    .build()
                    ))
                    .build();

            postedGeneralLedger.add(journalEntry);

            log.info("Invoice [{}] settled successfully with paymentId [{}] for ₹{}",
                    invoiceId, paymentId, amount);

            return WebhookProcessingResult.builder()
                    .paymentId(paymentId)
                    .invoiceId(invoiceId)
                    .flatNumber(flatNumber)
                    .disposition(PaymentDisposition.INVOICE_SETTLED)
                    .amountProcessed(amount)
                    .currentWalletBalance(wallet.getBalance())
                    .generatedJournalEntry(journalEntry)
                    .residentNotification(String.format("Payment of ₹%s received! Invoice %s marked PAID.",
                            amount, invoiceId))
                    .build();
        }

        // 3. Scenario B: Invoice is ALREADY PAID -> Delayed / Out-of-order webhook detected!
        log.warn("Invoice [{}] was already settled by [{}]. Delayed webhook [{}] arrived with ₹{}. Diverting to Advance Wallet.",
                invoiceId, invoice.getSettledPaymentId(), paymentId, amount);

        processedPaymentIds.add(paymentId);

        // Credit to Resident Advance Maintenance Wallet
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.getHistory().add(WalletTransaction.builder()
                .transactionId("WTX-" + UUID.randomUUID())
                .amount(amount)
                .paymentRef(paymentId)
                .utr(webhook.getUtr())
                .narration(String.format("Delayed payment for already-settled invoice %s", invoiceId))
                .timestamp(Instant.now())
                .build());

        // Double-entry GL for Advance Maintenance:
        // Debit: 1010 Gateway Clearing (Asset +)
        // Credit: 2100 Resident Advance Maintenance Liability (Liability +)
        ReconciliationJournalEntry journalEntry = ReconciliationJournalEntry.builder()
                .journalEntryId("JE-ADV-" + UUID.randomUUID())
                .transactionReference(paymentId)
                .entryType("ADVANCE_WALLET_CREDIT")
                .postedAt(Instant.now())
                .narration(String.format("Duplicate/delayed payment diverted to Advance Wallet for Flat %s (Inv: %s, UTR: %s)",
                        flatNumber, invoiceId, webhook.getUtr()))
                .lines(List.of(
                        JournalLine.builder()
                                .accountCode(COA_GATEWAY_CLEARING)
                                .accountName("Payment Gateway Clearing")
                                .debitAmount(amount)
                                .creditAmount(BigDecimal.ZERO)
                                .lineNarration("Gateway funds received for duplicate payment")
                                .build(),
                        JournalLine.builder()
                                .accountCode(COA_ADVANCE_MAINTENANCE_LIABILITY)
                                .accountName("Resident Advance Maintenance Liability")
                                .debitAmount(BigDecimal.ZERO)
                                .creditAmount(amount)
                                .lineNarration("Credit advance liability for Flat " + flatNumber)
                                .build()
                ))
                .build();

        postedGeneralLedger.add(journalEntry);

        String residentNotification = String.format(
                "Duplicate payment detected: We received your delayed payment of ₹%s (UTR: %s). " +
                "Since invoice %s was already paid, ₹%s has been credited to your Advance Maintenance Wallet. " +
                "New Wallet Balance: ₹%s (will auto-adjust next month).",
                amount, webhook.getUtr(), invoiceId, amount, wallet.getBalance()
        );

        log.info("Excess ₹{} successfully credited to Advance Wallet for Flat [{}]. New balance: ₹{}",
                amount, flatNumber, wallet.getBalance());

        return WebhookProcessingResult.builder()
                .paymentId(paymentId)
                .invoiceId(invoiceId)
                .flatNumber(flatNumber)
                .disposition(PaymentDisposition.CREDITED_TO_ADVANCE_WALLET)
                .amountProcessed(amount)
                .currentWalletBalance(wallet.getBalance())
                .generatedJournalEntry(journalEntry)
                .residentNotification(residentNotification)
                .build();
    }

    public void clear() {
        invoiceStore.clear();
        walletStore.clear();
        processedPaymentIds.clear();
        postedGeneralLedger.clear();
    }
}
