package com.manacommunity.api.payments.reconciliation;

import com.manacommunity.api.payments.reconciliation.model.BankSettlementRecord;
import com.manacommunity.api.payments.reconciliation.model.PaymentWebhookPayload;
import com.manacommunity.api.payments.reconciliation.model.ReconciliationJournalEntry;
import com.manacommunity.api.payments.reconciliation.model.ReconciliationReport;
import com.manacommunity.api.payments.reconciliation.service.DelayedWebhookHandler;
import com.manacommunity.api.payments.reconciliation.service.DelayedWebhookHandler.*;
import com.manacommunity.api.payments.reconciliation.service.TPlusOneBankReconciliationEngine;
import com.manacommunity.api.payments.reconciliation.service.TPlusOneBankReconciliationEngine.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentReconciliationEngineTest {

    private DelayedWebhookHandler webhookHandler;
    private TPlusOneBankReconciliationEngine reconciliationEngine;

    @BeforeEach
    void setUp() {
        webhookHandler = new DelayedWebhookHandler();
        reconciliationEngine = new TPlusOneBankReconciliationEngine();
    }

    @Test
    void testDelayedWebhookDivertsDuplicateExcessPaymentToAdvanceWallet() {
        String invoiceId = "INV-2026-11-001";
        String flatNumber = "A-402";
        BigDecimal billAmount = new BigDecimal("6500.00");

        // 1. Register invoice in system
        webhookHandler.registerInvoice(InvoiceRecord.builder()
                .invoiceId(invoiceId)
                .flatNumber(flatNumber)
                .residentId("RES-9912")
                .billAmount(billAmount)
                .paid(false)
                .build());

        // 2. Resident pays via Net Banking first (succeeds)
        PaymentWebhookPayload netBankingWebhook = PaymentWebhookPayload.builder()
                .eventId("EVT-NB-001")
                .eventType("payment.captured")
                .paymentId("pay_NB_99218")
                .invoiceId(invoiceId)
                .flatNumber(flatNumber)
                .paymentMethod("NETBANKING")
                .amount(billAmount)
                .utr("HDFC9912837101")
                .gatewayTimestamp(Instant.now().minusSeconds(3600))
                .build();

        WebhookProcessingResult firstResult = webhookHandler.processPaymentWebhook(netBankingWebhook);
        assertThat(firstResult.getDisposition()).isEqualTo(PaymentDisposition.INVOICE_SETTLED);
        assertThat(firstResult.getGeneratedJournalEntry().isBalanced()).isTrue();
        assertThat(firstResult.getCurrentWalletBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        InvoiceRecord invoiceAfterNb = webhookHandler.getInvoice(invoiceId);
        assertThat(invoiceAfterNb.isPaid()).isTrue();
        assertThat(invoiceAfterNb.getSettledPaymentId()).isEqualTo("pay_NB_99218");

        // 3. 45 Minutes Later: Delayed UPI Webhook arrives for the same invoice!
        PaymentWebhookPayload delayedUpiWebhook = PaymentWebhookPayload.builder()
                .eventId("EVT-UPI-002")
                .eventType("payment.captured")
                .paymentId("pay_UPI_88192")
                .invoiceId(invoiceId)
                .flatNumber(flatNumber)
                .paymentMethod("UPI")
                .amount(billAmount)
                .utr("ICICI228910283")
                .gatewayTimestamp(Instant.now().minusSeconds(2700))
                .build();

        WebhookProcessingResult delayedResult = webhookHandler.processPaymentWebhook(delayedUpiWebhook);

        // 4. Verify duplicate payment is safely diverted to Advance Maintenance Wallet
        assertThat(delayedResult.getDisposition()).isEqualTo(PaymentDisposition.CREDITED_TO_ADVANCE_WALLET);
        assertThat(delayedResult.getAmountProcessed()).isEqualByComparingTo(billAmount);
        assertThat(delayedResult.getCurrentWalletBalance()).isEqualByComparingTo(billAmount);

        AdvanceWallet wallet = webhookHandler.getWallet(flatNumber);
        assertThat(wallet.getBalance()).isEqualByComparingTo(billAmount);
        assertThat(wallet.getHistory()).hasSize(1);
        assertThat(wallet.getHistory().get(0).getUtr()).isEqualTo("ICICI228910283");

        // 5. Verify General Ledger is penny-exact and balanced
        ReconciliationJournalEntry advanceJe = delayedResult.getGeneratedJournalEntry();
        assertThat(advanceJe).isNotNull();
        assertThat(advanceJe.isBalanced()).isTrue();
        assertThat(advanceJe.getTotalDebits()).isEqualByComparingTo(billAmount);
        assertThat(advanceJe.getTotalCredits()).isEqualByComparingTo(billAmount);

        // Verify debit is to Gateway Clearing and credit is to Advance Liability (NOT Revenue)
        assertThat(advanceJe.getLines().get(0).getAccountCode()).isEqualTo(DelayedWebhookHandler.COA_GATEWAY_CLEARING);
        assertThat(advanceJe.getLines().get(0).getDebitAmount()).isEqualByComparingTo(billAmount);
        assertThat(advanceJe.getLines().get(1).getAccountCode()).isEqualTo(DelayedWebhookHandler.COA_ADVANCE_MAINTENANCE_LIABILITY);
        assertThat(advanceJe.getLines().get(1).getCreditAmount()).isEqualByComparingTo(billAmount);

        // 6. Verify resident notification
        assertThat(delayedResult.getResidentNotification())
                .contains("Duplicate payment detected")
                .contains("₹6500.00")
                .contains("Advance Maintenance Wallet")
                .contains("New Wallet Balance: ₹6500.00");
    }

    @Test
    void testWebhookIdempotencyIgnoresDuplicatePayload() {
        String invoiceId = "INV-2026-11-002";
        BigDecimal billAmount = new BigDecimal("4500.00");

        webhookHandler.registerInvoice(InvoiceRecord.builder()
                .invoiceId(invoiceId)
                .flatNumber("B-101")
                .billAmount(billAmount)
                .paid(false)
                .build());

        PaymentWebhookPayload payload = PaymentWebhookPayload.builder()
                .paymentId("pay_RETRY_12345")
                .invoiceId(invoiceId)
                .flatNumber("B-101")
                .amount(billAmount)
                .utr("UTR-9912-RETRY")
                .build();

        // First delivery: processes normally
        WebhookProcessingResult first = webhookHandler.processPaymentWebhook(payload);
        assertThat(first.getDisposition()).isEqualTo(PaymentDisposition.INVOICE_SETTLED);

        // Duplicate delivery (gateway retry): acknowledged without re-processing
        WebhookProcessingResult retry = webhookHandler.processPaymentWebhook(payload);
        assertThat(retry.getDisposition()).isEqualTo(PaymentDisposition.DUPLICATE_WEBHOOK_ACKNOWLEDGED);
        assertThat(webhookHandler.getPostedJournalEntries()).hasSize(1);
    }

    @Test
    void testTPlusOneBankReconciliationMatchesUtrsAndGeneratesBalancedEntries() {
        String utr1 = "CMS992144510001";
        BigDecimal gross = new BigDecimal("10000.00");
        BigDecimal mdr = new BigDecimal("15.00"); // 0.15%
        BigDecimal gst = new BigDecimal("2.70"); // 18% of 15
        BigDecimal net = new BigDecimal("9982.30"); // 10000 - 15 - 2.70

        // Register internal payment intent
        reconciliationEngine.registerPaymentIntent(PaymentIntent.builder()
                .paymentId("pay_ORD_551")
                .utr(utr1)
                .invoiceId("INV-2026-11-551")
                .flatNumber("C-302")
                .expectedGrossAmount(gross)
                .status("CAPTURED")
                .build());

        BankSettlementRecord record = BankSettlementRecord.builder()
                .settlementId("STL-001")
                .utr(utr1)
                .gatewayPaymentId("pay_ORD_551")
                .invoiceReference("INV-2026-11-551")
                .payerName("Deepak Sharma")
                .settlementDate(LocalDate.now())
                .grossAmount(gross)
                .mdrFee(mdr)
                .gstOnMdr(gst)
                .netSettledAmount(net)
                .narration("T+1 Razorpay Settlement UTR " + utr1)
                .bankAccount("ICICI-9921-CURRENT")
                .build();

        ReconciliationReport report = reconciliationEngine.reconcileSettlementBatch(
                "BATCH-T1-2026-11-28",
                List.of(record),
                new BigDecimal("0.0015")
        );

        assertThat(report.getTotalRecordsParsed()).isEqualTo(1);
        assertThat(report.getMatchedCount()).isEqualTo(1);
        assertThat(report.getOrphanCount()).isEqualTo(0);
        assertThat(report.getFeeVarianceCount()).isEqualTo(0);
        assertThat(report.getTotalGrossSettled()).isEqualByComparingTo(gross);
        assertThat(report.getTotalMdrDeducted()).isEqualByComparingTo(mdr);
        assertThat(report.getTotalGstDeducted()).isEqualByComparingTo(gst);
        assertThat(report.getTotalNetCreditedToBank()).isEqualByComparingTo(net);

        // Verify balanced double-entry accounting
        assertThat(report.getGeneratedJournalEntries()).hasSize(1);
        ReconciliationJournalEntry je = report.getGeneratedJournalEntries().get(0);
        assertThat(je.isBalanced()).isTrue();
        assertThat(je.getTotalDebits()).isEqualByComparingTo(gross);
        assertThat(je.getTotalCredits()).isEqualByComparingTo(gross);

        // Verify the 4 ledger legs:
        // Debit Bank: 9982.30
        assertThat(je.getLines().get(0).getAccountCode()).isEqualTo(TPlusOneBankReconciliationEngine.COA_BANK_OPERATING);
        assertThat(je.getLines().get(0).getDebitAmount()).isEqualByComparingTo(net);

        // Debit MDR: 15.00
        assertThat(je.getLines().get(1).getAccountCode()).isEqualTo(TPlusOneBankReconciliationEngine.COA_MDR_EXPENSE);
        assertThat(je.getLines().get(1).getDebitAmount()).isEqualByComparingTo(mdr);

        // Debit GST: 2.70
        assertThat(je.getLines().get(2).getAccountCode()).isEqualTo(TPlusOneBankReconciliationEngine.COA_INPUT_GST_CREDIT);
        assertThat(je.getLines().get(2).getDebitAmount()).isEqualByComparingTo(gst);

        // Credit Gateway Clearing: 10000.00
        assertThat(je.getLines().get(3).getAccountCode()).isEqualTo(TPlusOneBankReconciliationEngine.COA_GATEWAY_CLEARING);
        assertThat(je.getLines().get(3).getCreditAmount()).isEqualByComparingTo(gross);
    }

    @Test
    void testTPlusOneDetectsOrphanBankCreditsAndRoutesToSuspense() {
        String unknownUtr = "IMPS-UNM-99441";
        BigDecimal orphanAmount = new BigDecimal("12500.00");

        // Bank statement contains a direct NEFT/IMPS credit that has no matching payment intent
        BankSettlementRecord orphanRecord = BankSettlementRecord.builder()
                .settlementId("STL-ORPHAN-002")
                .utr(unknownUtr)
                .payerName("Suresh Kumar")
                .settlementDate(LocalDate.now())
                .grossAmount(orphanAmount)
                .mdrFee(BigDecimal.ZERO)
                .gstOnMdr(BigDecimal.ZERO)
                .netSettledAmount(orphanAmount)
                .narration("IMPS-P2A-SURESH KUMAR-FLAT 502 MAINTENANCE")
                .bankAccount("ICICI-9921-CURRENT")
                .build();

        ReconciliationReport report = reconciliationEngine.reconcileSettlementBatch(
                "BATCH-ORPHAN-TEST",
                List.of(orphanRecord),
                new BigDecimal("0.0015")
        );

        assertThat(report.getTotalRecordsParsed()).isEqualTo(1);
        assertThat(report.getMatchedCount()).isEqualTo(0);
        assertThat(report.getOrphanCount()).isEqualTo(1);
        assertThat(report.getOrphanCredits()).hasSize(1);

        // Suspense Journal Entry generated
        assertThat(report.getGeneratedJournalEntries()).hasSize(1);
        ReconciliationJournalEntry suspenseJe = report.getGeneratedJournalEntries().get(0);
        assertThat(suspenseJe.isBalanced()).isTrue();
        assertThat(suspenseJe.getEntryType()).isEqualTo("ORPHAN_SUSPENSE");

        // Debit Bank, Credit Orphan Suspense Liability
        assertThat(suspenseJe.getLines().get(0).getAccountCode()).isEqualTo(TPlusOneBankReconciliationEngine.COA_BANK_OPERATING);
        assertThat(suspenseJe.getLines().get(0).getDebitAmount()).isEqualByComparingTo(orphanAmount);
        assertThat(suspenseJe.getLines().get(1).getAccountCode()).isEqualTo(TPlusOneBankReconciliationEngine.COA_ORPHAN_SUSPENSE);
        assertThat(suspenseJe.getLines().get(1).getCreditAmount()).isEqualByComparingTo(orphanAmount);

        // Verify audit log detected potential flat 502
        assertThat(report.getAuditLogs().get(0))
                .contains("ORPHAN CREDIT detected")
                .contains("Potential Flat: 502");
    }

    @Test
    void testMdrFeeVarianceDetection() {
        String utr = "CMS-VARIANCE-009";
        BigDecimal gross = new BigDecimal("10000.00");
        BigDecimal excessiveMdr = new BigDecimal("40.00"); // 0.40% instead of agreed 0.15% (max ₹15)
        BigDecimal gst = new BigDecimal("7.20"); // 18% of 40
        BigDecimal net = new BigDecimal("9952.80");

        reconciliationEngine.registerPaymentIntent(PaymentIntent.builder()
                .paymentId("pay_VAR_101")
                .utr(utr)
                .invoiceId("INV-VAR-101")
                .expectedGrossAmount(gross)
                .status("CAPTURED")
                .build());

        BankSettlementRecord record = BankSettlementRecord.builder()
                .utr(utr)
                .grossAmount(gross)
                .mdrFee(excessiveMdr)
                .gstOnMdr(gst)
                .netSettledAmount(net)
                .build();

        ReconciliationReport report = reconciliationEngine.reconcileSettlementBatch(
                "BATCH-FEE-VARIANCE",
                List.of(record),
                new BigDecimal("0.0015") // Contractual max 0.15%
        );

        assertThat(report.getFeeVarianceCount()).isEqualTo(1);
        assertThat(report.getAuditLogs()).anyMatch(log ->
                log.contains("MDR fee variance detected") && log.contains("Bank charged ₹40.00, contractual ceiling is ₹15.00"));
    }
}
