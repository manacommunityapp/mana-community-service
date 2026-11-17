package com.manacommunity.api.transactioncore.listener;

import com.manacommunity.api.cfbos.accounting.dto.JournalEntryRequest;
import com.manacommunity.api.cfbos.accounting.engine.AccountingEngine;
import com.manacommunity.api.cfbos.accounting.repository.AccountRepository;
import com.manacommunity.api.cfbos.shared.enums.SourceModule;
import com.manacommunity.api.payments.repository.PaymentOrderRepository;
import com.manacommunity.api.transactioncore.event.GatewayPaymentCapturedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private final PaymentOrderRepository paymentOrderRepository;

    @Value("${app.cfbos.ledger.bank-account-code:1001}")
    private String bankAccountCode;

    @Value("${app.cfbos.ledger.income-account-code:4001}")
    private String incomeAccountCode;

    @Value("${app.cfbos.ledger.fee-income-account-code:4002}")
    private String feeIncomeAccountCode;

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

            // Fetch the stored order to get gross amount and convenience fee.
            var orderOpt = paymentOrderRepository.findById(event.getInternalOrderId());
            BigDecimal grossAmount = orderOpt.map(o -> o.getGrossAmount() != null
                    ? o.getGrossAmount() : o.getAmount()).orElse(event.getAmount());
            BigDecimal convenienceFee = orderOpt.map(o -> o.getConvenienceFeeAmount() != null
                    ? o.getConvenienceFeeAmount() : BigDecimal.ZERO).orElse(BigDecimal.ZERO);

            List<JournalEntryRequest.LineRequest> lines = new ArrayList<>();
            // Debit bank/wallet for gross amount collected
            lines.add(JournalEntryRequest.LineRequest.builder()
                    .accountCode(bankAccountCode)
                    .debitAmount(grossAmount)
                    .creditAmount(BigDecimal.ZERO)
                    .narration("Gateway receipt: " + event.getGatewayPaymentId())
                    .build());
            // Credit income account for the base amount
            lines.add(JournalEntryRequest.LineRequest.builder()
                    .accountCode(incomeAccountCode)
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(event.getAmount())
                    .narration("Payment income: " + event.getGatewayOrderId())
                    .build());
            // Credit fee income account separately if a convenience fee was charged
            if (convenienceFee.compareTo(BigDecimal.ZERO) > 0
                    && accountRepository.findByCode(feeIncomeAccountCode).isPresent()) {
                lines.add(JournalEntryRequest.LineRequest.builder()
                        .accountCode(feeIncomeAccountCode)
                        .debitAmount(BigDecimal.ZERO)
                        .creditAmount(convenienceFee)
                        .narration("Convenience fee: " + event.getGatewayOrderId())
                        .build());
            }

            JournalEntryRequest request = JournalEntryRequest.builder()
                    .entryDate(LocalDate.now())
                    .narration(event.getNarration())
                    .sourceModule(SourceModule.PAYMENT)
                    .sourceDocumentType(event.getGateway().name() + "_ORDER")
                    .sourceDocumentId(event.getInternalOrderId())
                    .lines(lines)
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
