package com.manacommunity.api.transaction.engine;

import com.manacommunity.api.cfbos.accounting.entity.JournalEntry;
import com.manacommunity.api.cfbos.invoice.entity.CfbosInvoice;
import com.manacommunity.api.cfbos.payment.entity.CfbosPayment;
import com.manacommunity.api.cfbos.payment.entity.CfbosReceipt;
import com.manacommunity.api.cfbos.payment.entity.CfbosRefund;
import com.manacommunity.api.cfbos.shared.exception.CfbosException;
import com.manacommunity.api.cfbos.shared.exception.CfbosResourceNotFoundException;
import com.manacommunity.api.transaction.entity.TransactionCoreRecord;
import com.manacommunity.api.transaction.enums.TransactionDomain;
import com.manacommunity.api.transaction.enums.TransactionPaymentMethod;
import com.manacommunity.api.transaction.enums.TransactionStatus;
import com.manacommunity.api.transaction.model.*;
import com.manacommunity.api.transaction.pillar.*;
import com.manacommunity.api.transaction.repository.TransactionCoreRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionCoreEngine {

    private final TransactionCoreRecordRepository transactionRepository;
    private final TransactionPaymentPillar paymentPillar;
    private final TransactionLedgerPillar ledgerPillar;
    private final TransactionSettlementPillar settlementPillar;
    private final TransactionInvoicePillar invoicePillar;
    private final TransactionReceiptPillar receiptPillar;
    private final TransactionRefundPillar refundPillar;
    private final TransactionReconciliationPillar reconciliationPillar;

    @Transactional
    public TransactionExecutionResult executeTransaction(TransactionIntent intent) {
        if (intent == null) {
            throw new CfbosException("Transaction intent cannot be null");
        }
        if (intent.getDomain() == null) {
            throw new CfbosException("Transaction domain is required");
        }
        if (intent.getPayerId() == null) {
            throw new CfbosException("Payer ID is required");
        }
        if (intent.getAmount() == null || intent.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CfbosException("Transaction amount must be positive");
        }

        // Idempotency check
        if (intent.getIdempotencyKey() != null && !intent.getIdempotencyKey().isBlank()) {
            var existing = transactionRepository.findByIdempotencyKey(intent.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("Returning existing transaction for idempotency key {}", intent.getIdempotencyKey());
                return mapToResult(existing.get());
            }
        }

        String year = String.valueOf(LocalDate.now().getYear());
        String txnNumber = "TXN-" + year + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        TransactionPaymentMethod method = intent.getPaymentMethod() != null
                ? intent.getPaymentMethod() : TransactionPaymentMethod.WALLET;

        // 1. Pillar: Settlement Split Calculation
        TransactionSplitDetail split = settlementPillar.calculateSplit(intent);

        // 2. Pillar: Payment Capture (Debits wallet or verifies gateway)
        CfbosPayment payment = paymentPillar.capturePayment(intent, txnNumber);

        // 3. Pillar: Invoice Generation
        CfbosInvoice invoice = invoicePillar.generateInvoice(intent, txnNumber);

        // 4. Pillar: Receipt Issuance
        CfbosReceipt receipt = receiptPillar.issueReceipt(intent, payment, txnNumber);

        // 5. Pillar: Ledger Posting (Double-entry journal)
        JournalEntry journalEntry = ledgerPillar.recordTransactionJournal(intent, txnNumber, split);

        // Initial transaction status
        TransactionStatus initialStatus = intent.isEscrowRequired()
                ? TransactionStatus.IN_ESCROW
                : TransactionStatus.CAPTURED;

        // 6. Direct Settlement (if not in escrow)
        if (!intent.isEscrowRequired() && intent.getPayeeId() != null && intent.getPayeeId() > 0) {
            settlementPillar.creditVendorSettlement(
                    intent.getPayeeId(),
                    intent.getCommunityId(),
                    split.getVendorPayout(),
                    txnNumber
            );
            initialStatus = TransactionStatus.SETTLED;
        }

        // Persist Core Record
        TransactionCoreRecord record = TransactionCoreRecord.builder()
                .transactionNumber(txnNumber)
                .domain(intent.getDomain())
                .status(initialStatus)
                .paymentMethod(method)
                .amount(intent.getAmount())
                .netAmount(intent.getAmount().subtract(intent.getDiscountAmount() != null ? intent.getDiscountAmount() : BigDecimal.ZERO))
                .taxAmount(split.getTaxAmount())
                .discountAmount(intent.getDiscountAmount() != null ? intent.getDiscountAmount() : BigDecimal.ZERO)
                .platformFee(split.getPlatformFee())
                .tdsAmount(split.getTdsAmount())
                .vendorPayoutAmount(split.getVendorPayout())
                .currency(intent.getCurrency() != null ? intent.getCurrency() : "INR")
                .payerId(intent.getPayerId())
                .payeeId(intent.getPayeeId())
                .communityId(intent.getCommunityId())
                .propertyId(intent.getPropertyId())
                .referenceType(intent.getReferenceType())
                .referenceId(intent.getReferenceId())
                .idempotencyKey(intent.getIdempotencyKey())
                .isEscrow(intent.isEscrowRequired())
                .escrowReleased(!intent.isEscrowRequired())
                .escrowReleasedAt(intent.isEscrowRequired() ? null : LocalDateTime.now())
                .paymentId(payment != null ? payment.getId() : null)
                .invoiceId(invoice != null ? invoice.getId() : null)
                .receiptId(receipt != null ? receipt.getId() : null)
                .journalEntryId(journalEntry != null ? journalEntry.getId() : null)
                .gatewayReference(intent.getGatewayReference())
                .narration(intent.getNarration())
                .build();

        record = transactionRepository.save(record);
        log.info("Successfully processed MANA Transaction Core record {} for domain {}", txnNumber, intent.getDomain());
        return mapToResult(record);
    }

    @Transactional
    public TransactionExecutionResult releaseEscrow(TransactionEscrowReleaseRequest request) {
        TransactionCoreRecord txn = transactionRepository.findByTransactionNumber(request.getTransactionNumber())
                .orElseThrow(() -> new CfbosResourceNotFoundException("Transaction " + request.getTransactionNumber(), 0L));

        if (!txn.isEscrow() || txn.getStatus() != TransactionStatus.IN_ESCROW) {
            throw new CfbosException("Transaction is not currently in escrow: " + txn.getStatus());
        }

        TransactionSplitDetail split = TransactionSplitDetail.builder()
                .grossAmount(txn.getAmount())
                .platformFee(txn.getPlatformFee())
                .tdsAmount(txn.getTdsAmount())
                .vendorPayout(txn.getVendorPayoutAmount())
                .taxAmount(txn.getTaxAmount())
                .build();

        // 1. Post Escrow Release Journal
        ledgerPillar.recordEscrowReleaseJournal(txn.getTransactionNumber(), split);

        // 2. Credit Vendor Settlement
        if (txn.getPayeeId() != null && txn.getPayeeId() > 0) {
            settlementPillar.creditVendorSettlement(
                    txn.getPayeeId(),
                    txn.getCommunityId(),
                    txn.getVendorPayoutAmount(),
                    txn.getTransactionNumber()
            );
        }

        txn.setEscrowReleased(true);
        txn.setEscrowReleasedAt(LocalDateTime.now());
        txn.setStatus(TransactionStatus.SETTLED);

        txn = transactionRepository.save(txn);
        log.info("Escrow successfully released for transaction {}", txn.getTransactionNumber());
        return mapToResult(txn);
    }

    @Transactional
    public TransactionExecutionResult refundTransaction(TransactionRefundRequest request) {
        TransactionCoreRecord txn = transactionRepository.findByTransactionNumber(request.getTransactionNumber())
                .orElseThrow(() -> new CfbosResourceNotFoundException("Transaction " + request.getTransactionNumber(), 0L));

        if (txn.getStatus() == TransactionStatus.REFUNDED) {
            throw new CfbosException("Transaction is already refunded");
        }

        CfbosRefund refund = refundPillar.executeRefund(txn, request);
        txn.setRefundId(refund.getId());

        BigDecimal refundAmt = request.getRefundAmount() != null ? request.getRefundAmount() : txn.getAmount();
        if (refundAmt.compareTo(txn.getAmount()) >= 0) {
            txn.setStatus(TransactionStatus.REFUNDED);
        } else {
            txn.setStatus(TransactionStatus.PARTIALLY_REFUNDED);
        }

        txn = transactionRepository.save(txn);
        log.info("Refund executed for transaction {}: refundId={}", txn.getTransactionNumber(), refund.getId());
        return mapToResult(txn);
    }

    @Transactional
    public TransactionExecutionResult reconcileTransaction(String transactionNumber, String utr, String gatewayRef) {
        TransactionCoreRecord record = reconciliationPillar.reconcileTransaction(transactionNumber, utr, gatewayRef);
        return mapToResult(record);
    }

    public TransactionExecutionResult getTransaction(String transactionNumber) {
        TransactionCoreRecord txn = transactionRepository.findByTransactionNumber(transactionNumber)
                .orElseThrow(() -> new CfbosResourceNotFoundException("Transaction " + transactionNumber, 0L));
        return mapToResult(txn);
    }

    public Page<TransactionExecutionResult> getTransactionsByPayer(Long payerId, Pageable pageable) {
        return transactionRepository.findByPayerId(payerId, pageable).map(this::mapToResult);
    }

    public Page<TransactionExecutionResult> getTransactionsByDomain(TransactionDomain domain, Pageable pageable) {
        return transactionRepository.findByDomain(domain, pageable).map(this::mapToResult);
    }

    private TransactionExecutionResult mapToResult(TransactionCoreRecord r) {
        return TransactionExecutionResult.builder()
                .id(r.getId())
                .transactionNumber(r.getTransactionNumber())
                .domain(r.getDomain())
                .status(r.getStatus())
                .paymentMethod(r.getPaymentMethod())
                .amount(r.getAmount())
                .netAmount(r.getNetAmount())
                .taxAmount(r.getTaxAmount())
                .platformFee(r.getPlatformFee())
                .vendorPayout(r.getVendorPayoutAmount())
                .payerId(r.getPayerId())
                .payeeId(r.getPayeeId())
                .paymentId(r.getPaymentId())
                .invoiceId(r.getInvoiceId())
                .receiptId(r.getReceiptId())
                .journalEntryId(r.getJournalEntryId())
                .settlementId(r.getSettlementId())
                .refundId(r.getRefundId())
                .isEscrow(r.isEscrow())
                .escrowReleased(r.isEscrowReleased())
                .gatewayReference(r.getGatewayReference())
                .narration(r.getNarration())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
