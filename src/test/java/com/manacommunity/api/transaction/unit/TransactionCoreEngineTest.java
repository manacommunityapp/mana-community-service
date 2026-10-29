package com.manacommunity.api.transaction.unit;

import com.manacommunity.api.cfbos.accounting.entity.JournalEntry;
import com.manacommunity.api.cfbos.invoice.entity.CfbosInvoice;
import com.manacommunity.api.cfbos.payment.entity.CfbosPayment;
import com.manacommunity.api.cfbos.payment.entity.CfbosReceipt;
import com.manacommunity.api.cfbos.payment.entity.CfbosRefund;
import com.manacommunity.api.transaction.engine.TransactionCoreEngine;
import com.manacommunity.api.transaction.entity.TransactionCoreRecord;
import com.manacommunity.api.transaction.enums.TransactionDomain;
import com.manacommunity.api.transaction.enums.TransactionPaymentMethod;
import com.manacommunity.api.transaction.enums.TransactionStatus;
import com.manacommunity.api.transaction.model.*;
import com.manacommunity.api.transaction.pillar.*;
import com.manacommunity.api.transaction.repository.TransactionCoreRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionCoreEngineTest {

    @Mock private TransactionCoreRecordRepository transactionRepository;
    @Mock private TransactionPaymentPillar paymentPillar;
    @Mock private TransactionLedgerPillar ledgerPillar;
    @Mock private TransactionSettlementPillar settlementPillar;
    @Mock private TransactionInvoicePillar invoicePillar;
    @Mock private TransactionReceiptPillar receiptPillar;
    @Mock private TransactionRefundPillar refundPillar;
    @Mock private TransactionReconciliationPillar reconciliationPillar;

    private TransactionCoreEngine engine;

    @BeforeEach
    void setUp() {
        engine = new TransactionCoreEngine(
                transactionRepository,
                paymentPillar,
                ledgerPillar,
                settlementPillar,
                invoicePillar,
                receiptPillar,
                refundPillar,
                reconciliationPillar
        );
    }

    @Test
    @DisplayName("Execute transaction triggers all 7 core pillars and persists record")
    void executeTransactionFullLifecycle() {
        TransactionIntent intent = TransactionIntent.builder()
                .domain(TransactionDomain.MARKETPLACE)
                .payerId(101L)
                .payeeId(202L)
                .communityId(1L)
                .amount(new BigDecimal("1000.00"))
                .taxAmount(new BigDecimal("180.00"))
                .paymentMethod(TransactionPaymentMethod.WALLET)
                .isEscrowRequired(true)
                .narration("Test Marketplace purchase")
                .build();

        TransactionSplitDetail split = TransactionSplitDetail.builder()
                .grossAmount(new BigDecimal("1000.00"))
                .platformFee(new BigDecimal("50.00"))
                .tdsAmount(new BigDecimal("10.00"))
                .vendorPayout(new BigDecimal("760.00"))
                .taxAmount(new BigDecimal("180.00"))
                .build();

        CfbosPayment payment = CfbosPayment.builder().id(11L).build();
        CfbosInvoice invoice = CfbosInvoice.builder().id(22L).build();
        CfbosReceipt receipt = CfbosReceipt.builder().id(33L).build();
        JournalEntry journal = JournalEntry.builder().id(44L).build();

        when(settlementPillar.calculateSplit(any())).thenReturn(split);
        when(paymentPillar.capturePayment(any(), any())).thenReturn(payment);
        when(invoicePillar.generateInvoice(any(), any())).thenReturn(invoice);
        when(receiptPillar.issueReceipt(any(), any(), any())).thenReturn(receipt);
        when(ledgerPillar.recordTransactionJournal(any(), any(), any())).thenReturn(journal);
        when(transactionRepository.save(any(TransactionCoreRecord.class))).thenAnswer(i -> {
            TransactionCoreRecord r = i.getArgument(0);
            r.setId(1L);
            return r;
        });

        TransactionExecutionResult result = engine.executeTransaction(intent);

        assertThat(result).isNotNull();
        assertThat(result.getDomain()).isEqualTo(TransactionDomain.MARKETPLACE);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.IN_ESCROW);
        assertThat(result.getPaymentId()).isEqualTo(11L);
        assertThat(result.getInvoiceId()).isEqualTo(22L);
        assertThat(result.getReceiptId()).isEqualTo(33L);
        assertThat(result.getJournalEntryId()).isEqualTo(44L);

        verify(paymentPillar).capturePayment(eq(intent), any());
        verify(invoicePillar).generateInvoice(eq(intent), any());
        verify(receiptPillar).issueReceipt(eq(intent), eq(payment), any());
        verify(ledgerPillar).recordTransactionJournal(eq(intent), any(), eq(split));
    }

    @Test
    @DisplayName("Release escrow triggers vendor payout and ledger posting")
    void releaseEscrowSuccess() {
        TransactionCoreRecord record = TransactionCoreRecord.builder()
                .id(1L)
                .transactionNumber("TXN-2026-TEST01")
                .domain(TransactionDomain.MARKETPLACE)
                .status(TransactionStatus.IN_ESCROW)
                .amount(new BigDecimal("1000.00"))
                .platformFee(new BigDecimal("50.00"))
                .tdsAmount(new BigDecimal("10.00"))
                .vendorPayoutAmount(new BigDecimal("760.00"))
                .taxAmount(new BigDecimal("180.00"))
                .payerId(101L)
                .payeeId(202L)
                .communityId(1L)
                .isEscrow(true)
                .escrowReleased(false)
                .build();

        when(transactionRepository.findByTransactionNumber("TXN-2026-TEST01")).thenReturn(Optional.of(record));
        when(transactionRepository.save(any(TransactionCoreRecord.class))).thenAnswer(i -> i.getArgument(0));

        TransactionExecutionResult result = engine.releaseEscrow(TransactionEscrowReleaseRequest.builder()
                .transactionNumber("TXN-2026-TEST01")
                .reason("Handover pass verified")
                .build());

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.SETTLED);
        assertThat(result.isEscrowReleased()).isTrue();

        verify(ledgerPillar).recordEscrowReleaseJournal(eq("TXN-2026-TEST01"), any());
        verify(settlementPillar).creditVendorSettlement(eq(202L), eq(1L), eq(new BigDecimal("760.00")), eq("TXN-2026-TEST01"));
    }

    @Test
    @DisplayName("Refund transaction updates record to REFUNDED")
    void refundTransactionSuccess() {
        TransactionCoreRecord record = TransactionCoreRecord.builder()
                .id(1L)
                .transactionNumber("TXN-2026-REF01")
                .domain(TransactionDomain.EVENTS)
                .status(TransactionStatus.CAPTURED)
                .amount(new BigDecimal("500.00"))
                .payerId(101L)
                .build();

        CfbosRefund refund = CfbosRefund.builder().id(99L).build();

        when(transactionRepository.findByTransactionNumber("TXN-2026-REF01")).thenReturn(Optional.of(record));
        when(refundPillar.executeRefund(any(), any())).thenReturn(refund);
        when(transactionRepository.save(any(TransactionCoreRecord.class))).thenAnswer(i -> i.getArgument(0));

        TransactionExecutionResult result = engine.refundTransaction(TransactionRefundRequest.builder()
                .transactionNumber("TXN-2026-REF01")
                .refundAmount(new BigDecimal("500.00"))
                .reason("Event cancelled")
                .refundToWallet(true)
                .build());

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
        assertThat(result.getRefundId()).isEqualTo(99L);
    }
}
