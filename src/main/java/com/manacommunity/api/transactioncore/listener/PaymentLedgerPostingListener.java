package com.manacommunity.api.transactioncore.listener;

import com.manacommunity.api.cfbos.accounting.dto.JournalEntryRequest;
import com.manacommunity.api.cfbos.accounting.engine.AccountingEngine;
import com.manacommunity.api.cfbos.accounting.repository.AccountRepository;
import com.manacommunity.api.cfbos.shared.enums.SourceModule;
import com.manacommunity.api.transactioncore.event.GatewayPaymentCapturedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.List;

/**
 * Posts a double-entry journal after a gateway payment is confirmed.
 *
 * <p>Runs AFTER_COMMIT so a ledger failure can never roll back the payment.
 * Configure account codes in application.properties:
 *   app.cfbos.ledger.bank-account-code (default "1001")
 *   app.cfbos.ledger.income-account-code (default "4001")
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentLedgerPostingListener {

    private final AccountingEngine accountingEngine;
    private final AccountRepository accountRepository;

    @Value("${app.cfbos.ledger.bank-account-code:1001}")
    private String bankAccountCode;

    @Value("${app.cfbos.ledger.income-account-code:4001}")
    private String incomeAccountCode;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCaptured(GatewayPaymentCapturedEvent event) {
        try {
            // Skip silently if the chart of accounts has not been set up yet.
            if (accountRepository.findByCode(bankAccountCode).isEmpty()) {
                log.warn("[LEDGER] Bank account code '{}' not found — skipping ledger posting for order {}",
                        bankAccountCode, event.getGatewayOrderId());
                return;
            }
            if (accountRepository.findByCode(incomeAccountCode).isEmpty()) {
                log.warn("[LEDGER] Income account code '{}' not found — skipping ledger posting for order {}",
                        incomeAccountCode, event.getGatewayOrderId());
                return;
            }

            JournalEntryRequest request = JournalEntryRequest.builder()
                    .entryDate(LocalDate.now())
                    .narration(event.getNarration())
                    .sourceModule(SourceModule.PAYMENT)
                    .sourceDocumentType(event.getGateway().name() + "_ORDER")
                    .sourceDocumentId(event.getInternalOrderId())
                    .lines(List.of(
                            JournalEntryRequest.LineRequest.builder()
                                    .accountCode(bankAccountCode)
                                    .debitAmount(event.getAmount())
                                    .creditAmount(java.math.BigDecimal.ZERO)
                                    .narration("Gateway receipt: " + event.getGatewayPaymentId())
                                    .build(),
                            JournalEntryRequest.LineRequest.builder()
                                    .accountCode(incomeAccountCode)
                                    .debitAmount(java.math.BigDecimal.ZERO)
                                    .creditAmount(event.getAmount())
                                    .narration("Payment income: " + event.getGatewayOrderId())
                                    .build()
                    ))
                    .build();

            accountingEngine.createAndPostJournalEntry(request);
            log.info("[LEDGER] Posted journal for {} order={} amount={}",
                    event.getGateway(), event.getGatewayOrderId(), event.getAmount());

        } catch (Exception e) {
            // Ledger failures are logged but must never affect payment status.
            log.error("[LEDGER] Failed to post journal for {} order={}: {}",
                    event.getGateway(), event.getGatewayOrderId(), e.getMessage(), e);
        }
    }
}
