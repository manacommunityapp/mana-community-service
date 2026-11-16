package com.manacommunity.api.transactioncore.adapter;

import com.manacommunity.api.transactioncore.dto.*;
import com.manacommunity.api.transactioncore.enums.PaymentRail;
import com.manacommunity.api.transactioncore.enums.RefundDestination;
import com.manacommunity.api.transactioncore.enums.TransactionDomain;
import com.manacommunity.api.transactioncore.service.ManaTransactionCoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Handles Finance module invoice and expense payments via the transactioncore engine.
 * The Finance module covers general business invoices (B2B/B2C), vendor expense payments,
 * purchase orders, and credit/debit notes — distinct from CFBOS maintenance billing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinanceModuleTransactionAdapter {

    private final ManaTransactionCoreService transactionCore;

    /**
     * Creates a payment intent for collecting payment on a Finance module invoice
     * (invoices raised by the community entity to vendors or members for services rendered).
     *
     * @param invoiceRef      finance invoice number/reference
     * @param invoiceId       database primary key of the finance invoice record
     * @param payerId         the paying party's user ID (vendor, member, or admin)
     * @param payerName       payer display name
     * @param payerEmail      payer email for receipt
     * @param payerPhone      payer phone for receipt
     * @param invoiceAmount   total invoice amount — use BigDecimal, never float/double
     * @param communityId     community for multi-tenant isolation
     */
    public PaymentIntentResponse processInvoicePayment(
            String invoiceRef,
            Long invoiceId,
            Long payerId,
            String payerName,
            String payerEmail,
            String payerPhone,
            BigDecimal invoiceAmount,
            Long communityId) {

        log.info("Creating finance invoice payment intent invoiceRef={} payerId={} amount={}",
                invoiceRef, payerId, invoiceAmount);

        PaymentIntentRequest request = PaymentIntentRequest.builder()
                .idempotencyKey("FIN-INV-" + invoiceRef)
                .domain(TransactionDomain.VENDOR)
                .orderReferenceId(invoiceRef)
                .communityId(communityId)
                .payerId(payerId)
                .payerName(payerName)
                .payerEmail(payerEmail)
                .payerPhone(payerPhone)
                .title("Finance Invoice Payment - " + invoiceRef)
                .amount(invoiceAmount)
                .preferredRail(PaymentRail.UPI)
                .holdInEscrow(false)
                .metadata(Map.of(
                        "invoiceId", invoiceId,
                        "invoiceRef", invoiceRef,
                        "module", "FINANCE"))
                .remarks("Finance module invoice payment for ref " + invoiceRef)
                .build();

        return transactionCore.createPaymentIntent(request);
    }

    /**
     * Creates a payment intent for disbursing a vendor expense payment
     * (community pays a vendor for services or goods rendered).
     *
     * @param expenseRef     finance expense/PO reference
     * @param expenseId      database primary key of the finance expense record
     * @param vendorId       vendor user ID receiving the payment
     * @param vendorName     vendor display name
     * @param vendorPhone    vendor phone
     * @param expenseAmount  amount to disburse — use BigDecimal
     * @param communityId    community for multi-tenant isolation
     */
    public PaymentIntentResponse processVendorExpensePayment(
            String expenseRef,
            Long expenseId,
            Long vendorId,
            String vendorName,
            String vendorPhone,
            BigDecimal expenseAmount,
            Long communityId) {

        log.info("Creating vendor expense payment intent expenseRef={} vendorId={} amount={}",
                expenseRef, vendorId, expenseAmount);

        PaymentSplit vendorSplit = PaymentSplit.builder()
                .recipientType("VENDOR")
                .recipientId(vendorId)
                .amount(expenseAmount)
                .narration("Expense payment for " + expenseRef + " to vendor " + vendorName)
                .build();

        PaymentIntentRequest request = PaymentIntentRequest.builder()
                .idempotencyKey("FIN-EXP-" + expenseRef)
                .domain(TransactionDomain.VENDOR)
                .orderReferenceId(expenseRef)
                .communityId(communityId)
                .payerId(vendorId)
                .payerName(vendorName)
                .payerPhone(vendorPhone)
                .title("Vendor Expense Payment - " + expenseRef)
                .amount(expenseAmount)
                .preferredRail(PaymentRail.UPI)
                .holdInEscrow(false)
                .splits(java.util.List.of(vendorSplit))
                .metadata(Map.of(
                        "expenseId", expenseId,
                        "expenseRef", expenseRef,
                        "vendorId", vendorId,
                        "module", "FINANCE"))
                .remarks("Vendor expense disbursement for ref " + expenseRef)
                .build();

        return transactionCore.createPaymentIntent(request);
    }

    /**
     * Creates a payment intent for a Purchase Order advance payment.
     * PO advance is held in escrow until goods/services are confirmed delivered.
     */
    public PaymentIntentResponse processPurchaseOrderAdvance(
            String poRef,
            Long poId,
            Long vendorId,
            String vendorName,
            String vendorPhone,
            BigDecimal advanceAmount,
            Long communityId) {

        log.info("Creating PO advance payment intent poRef={} vendorId={} amount={}",
                poRef, vendorId, advanceAmount);

        PaymentIntentRequest request = PaymentIntentRequest.builder()
                .idempotencyKey("FIN-PO-" + poRef)
                .domain(TransactionDomain.VENDOR)
                .orderReferenceId(poRef)
                .communityId(communityId)
                .payerId(vendorId)
                .payerName(vendorName)
                .payerPhone(vendorPhone)
                .title("PO Advance Payment - " + poRef)
                .amount(advanceAmount)
                .preferredRail(PaymentRail.UPI)
                .holdInEscrow(true)
                .escrowReleaseCondition(com.manacommunity.api.transactioncore.enums.EscrowReleaseCondition.MANUAL_APPROVAL)
                .metadata(Map.of(
                        "poId", poId,
                        "poRef", poRef,
                        "vendorId", vendorId,
                        "module", "FINANCE"))
                .remarks("Purchase order advance for PO " + poRef + " — held until delivery confirmation")
                .build();

        return transactionCore.createPaymentIntent(request);
    }

    /**
     * Processes a credit note refund back to the payer's wallet.
     */
    public RefundResult processCreditNoteRefund(String intentId, BigDecimal creditAmount, String creditNoteRef) {
        log.info("Processing credit note refund intentId={} amount={} ref={}", intentId, creditAmount, creditNoteRef);

        RefundRequest request = RefundRequest.builder()
                .intentId(intentId)
                .refundAmount(creditAmount)
                .destination(RefundDestination.WALLET_INSTANT)
                .reason("Credit note issued: " + creditNoteRef)
                .requestedBy("SYSTEM_FINANCE_MODULE")
                .build();

        return transactionCore.processRefund(request);
    }
}
