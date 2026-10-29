package com.manacommunity.api.transaction.pillar;

import com.manacommunity.api.transaction.entity.TransactionCoreRecord;
import com.manacommunity.api.transaction.repository.TransactionCoreRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionReconciliationPillar {

    private final TransactionCoreRecordRepository transactionRepository;

    @Transactional
    public TransactionCoreRecord reconcileTransaction(String transactionNumber, String utrNumber, String gatewayRef) {
        TransactionCoreRecord txn = transactionRepository.findByTransactionNumber(transactionNumber)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionNumber));

        txn.setReconciliationStatus("RECONCILED");
        txn.setReconciliationUtr(utrNumber);
        if (gatewayRef != null && !gatewayRef.isBlank()) {
            txn.setGatewayReference(gatewayRef);
        }
        txn.setReconciledAt(LocalDateTime.now());

        log.info("Transaction {} successfully reconciled with UTR {}", transactionNumber, utrNumber);
        return transactionRepository.save(txn);
    }
}
