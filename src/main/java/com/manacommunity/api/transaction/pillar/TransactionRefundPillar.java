package com.manacommunity.api.transaction.pillar;

import com.manacommunity.api.cfbos.accounting.engine.AccountingEngine;
import com.manacommunity.api.cfbos.payment.entity.CfbosRefund;
import com.manacommunity.api.cfbos.payment.enums.PaymentMethodType;
import com.manacommunity.api.cfbos.payment.enums.PaymentStatus;
import com.manacommunity.api.cfbos.payment.repository.CfbosRefundRepository;
import com.manacommunity.api.cfbos.shared.enums.DocumentType;
import com.manacommunity.api.cfbos.shared.sequence.service.DocumentSequenceService;
import com.manacommunity.api.cfbos.wallet.engine.WalletEngine;
import com.manacommunity.api.cfbos.wallet.enums.WalletTransactionType;
import com.manacommunity.api.transaction.entity.TransactionCoreRecord;
import com.manacommunity.api.transaction.model.TransactionRefundRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionRefundPillar {

    private final CfbosRefundRepository refundRepository;
    private final WalletEngine walletEngine;
    private final AccountingEngine accountingEngine;
    private final DocumentSequenceService documentSequenceService;

    @Transactional
    public CfbosRefund executeRefund(TransactionCoreRecord txn, TransactionRefundRequest request) {
        BigDecimal refundAmount = request.getRefundAmount() != null ? request.getRefundAmount() : txn.getAmount();
        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());

        String refundNumber;
        try {
            refundNumber = documentSequenceService.nextNumber(DocumentType.CREDIT_NOTE, year);
            refundNumber = "REF-" + refundNumber.replace("CN-", "").replace("DOC-", "");
        } catch (Exception e) {
            refundNumber = "REF-" + year + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        // 1. Credit back to resident wallet
        if (request.isRefundToWallet() || txn.getPayerId() != null) {
            walletEngine.creditWallet(
                    txn.getPayerId(),
                    refundAmount,
                    WalletTransactionType.REFUND,
                    "TRANSACTION_REFUND",
                    txn.getId(),
                    "Refund for " + txn.getDomain() + " [" + txn.getTransactionNumber() + "]: " + request.getReason()
            );
        }

        // 2. Persist CfbosRefund
        CfbosRefund refund = CfbosRefund.builder()
                .refundNumber(refundNumber)
                .refundDate(now)
                .paymentId(txn.getPaymentId() != null ? txn.getPaymentId() : 0L)
                .residentId(txn.getPayerId())
                .amount(refundAmount)
                .reason(request.getReason() != null ? request.getReason() : "Customer requested refund")
                .refundMethod(PaymentMethodType.WALLET)
                .status(PaymentStatus.SUCCESS)
                .build();

        refund = refundRepository.save(refund);

        // 3. Reverse journal entry if present
        if (txn.getJournalEntryId() != null) {
            try {
                accountingEngine.reverseJournalEntry(txn.getJournalEntryId(), "Refund processed: " + refundNumber);
            } catch (Exception e) {
                log.warn("Could not auto-reverse journal entry {} for refund {}: {}",
                        txn.getJournalEntryId(), refundNumber, e.getMessage());
            }
        }

        return refund;
    }
}
