package com.manacommunity.api.finance.ledger;

import com.manacommunity.api.billing.event.InvoiceIssuedEvent;
import com.manacommunity.api.billing.event.InvoicePaymentReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Translates billing invoice lifecycle events into double-entry ledger rows.
 *
 * <ul>
 *   <li>Invoice issued  → DR Accounts Receivable (1100) / CR Maintenance Income (3100)</li>
 *   <li>Payment received → DR Cash / Bank (1010)         / CR Accounts Receivable (1100)</li>
 * </ul>
 *
 * Runs asynchronously so the billing transaction is never held up by ledger I/O.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceLedgerListener {

    private static final String ACCOUNT_RECEIVABLE = "1100";
    private static final String ACCOUNT_CASH_BANK   = "1010";
    private static final String ACCOUNT_INCOME      = "3100";

    private final GeneralLedgerEntryService ledgerEntryService;

    /**
     * When an invoice is issued, record the revenue accrual:
     * DR Accounts Receivable / CR Maintenance Income.
     */
    @Async
    @EventListener
    @Transactional
    public void onInvoiceIssued(InvoiceIssuedEvent event) {
        log.info("Ledger: invoice issued event for invoiceId={} community={}",
                event.invoiceId(), event.communityId());
        ledgerEntryService.postDoubleEntry(
                event.communityId(),
                ACCOUNT_RECEIVABLE,
                ACCOUNT_INCOME,
                event.amount(),
                "Invoice issued: " + event.invoiceNumber(),
                "INVOICE",
                String.valueOf(event.invoiceId()),
                event.dueDate() != null ? event.dueDate() : LocalDate.now()
        );
    }

    /**
     * When payment is received, clear the receivable and recognise cash:
     * DR Cash / Bank / CR Accounts Receivable.
     */
    @Async
    @EventListener
    @Transactional
    public void onInvoicePaymentReceived(InvoicePaymentReceivedEvent event) {
        log.info("Ledger: payment received event for invoiceId={} community={}",
                event.invoiceId(), event.communityId());
        ledgerEntryService.postDoubleEntry(
                event.communityId(),
                ACCOUNT_CASH_BANK,
                ACCOUNT_RECEIVABLE,
                event.paidAmount(),
                "Payment received for: " + event.invoiceNumber(),
                "PAYMENT",
                event.paymentReference() != null ? event.paymentReference()
                        : String.valueOf(event.invoiceId()),
                LocalDate.now()
        );
    }
}
