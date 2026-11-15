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
 * Handles CFBOS maintenance billing invoice payments via the transactioncore engine.
 * Used for community maintenance charges, utility bills, and recurring property levies.
 * All amounts validated server-side — frontend may never confirm payment success directly.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingTransactionAdapter {

    private final ManaTransactionCoreService transactionCore;

    /**
     * Creates a payment intent for a CFBOS maintenance billing invoice.
     * No escrow — maintenance payments are captured immediately.
     *
     * @param invoiceNumber  CFBOS invoice number (e.g., INV-2024-001)
     * @param invoiceId      database primary key of the cfbos_invoice record
     * @param residentId     the paying resident's user ID
     * @param residentName   resident display name
     * @param residentPhone  resident phone for receipt
     * @param unitNumber     flat/unit number (e.g., A-101)
     * @param amount         invoice amount in INR — NEVER use floating point; pass BigDecimal
     * @param communityId    community for multi-tenant isolation
     */
    public PaymentIntentResponse processMaintenanceInvoicePayment(
            String invoiceNumber,
            Long invoiceId,
            Long residentId,
            String residentName,
            String residentPhone,
            String unitNumber,
            BigDecimal amount,
            Long communityId) {

        log.info("Creating billing payment intent invoiceNumber={} residentId={} amount={}",
                invoiceNumber, residentId, amount);

        PaymentIntentRequest request = PaymentIntentRequest.builder()
                .idempotencyKey("BILL-" + invoiceNumber)
                .domain(TransactionDomain.COMMUNITY_BILLING)
                .orderReferenceId(invoiceNumber)
                .communityId(communityId)
                .payerId(residentId)
                .payerName(residentName)
                .payerPhone(residentPhone)
                .payerUnit(unitNumber)
                .title("Maintenance Bill Payment - " + unitNumber)
                .amount(amount)
                .preferredRail(PaymentRail.UPI)
                .holdInEscrow(false)
                .metadata(Map.of(
                        "invoiceId", invoiceId,
                        "invoiceNumber", invoiceNumber,
                        "unitNumber", unitNumber))
                .remarks("Maintenance billing payment for invoice " + invoiceNumber)
                .build();

        return transactionCore.createPaymentIntent(request);
    }

    /**
     * Creates a payment intent for a partial payment against a maintenance invoice.
     * Partial payments are allowed — the billing engine tracks outstanding balance.
     */
    public PaymentIntentResponse processPartialInvoicePayment(
            String invoiceNumber,
            Long invoiceId,
            Long residentId,
            String residentName,
            String residentPhone,
            String unitNumber,
            BigDecimal partialAmount,
            BigDecimal totalDue,
            Long communityId) {

        log.info("Creating partial billing payment invoiceNumber={} partial={} of total={}",
                invoiceNumber, partialAmount, totalDue);

        PaymentIntentRequest request = PaymentIntentRequest.builder()
                .idempotencyKey("BILL-PART-" + invoiceNumber + "-" + partialAmount.toPlainString())
                .domain(TransactionDomain.COMMUNITY_BILLING)
                .orderReferenceId(invoiceNumber)
                .communityId(communityId)
                .payerId(residentId)
                .payerName(residentName)
                .payerPhone(residentPhone)
                .payerUnit(unitNumber)
                .title("Partial Maintenance Payment - " + unitNumber)
                .amount(partialAmount)
                .preferredRail(PaymentRail.UPI)
                .holdInEscrow(false)
                .metadata(Map.of(
                        "invoiceId", invoiceId,
                        "invoiceNumber", invoiceNumber,
                        "totalDue", totalDue,
                        "partialAmount", partialAmount,
                        "isPartialPayment", true))
                .remarks("Partial payment for invoice " + invoiceNumber + " — balance remaining: "
                        + totalDue.subtract(partialAmount).toPlainString())
                .build();

        return transactionCore.createPaymentIntent(request);
    }

    /**
     * Issues a wallet refund for an overpaid or cancelled maintenance invoice.
     */
    public RefundResult refundMaintenancePayment(String intentId, BigDecimal refundAmount, String reason) {
        log.info("Refunding maintenance payment intentId={} amount={}", intentId, refundAmount);

        RefundRequest request = RefundRequest.builder()
                .intentId(intentId)
                .refundAmount(refundAmount)
                .destination(RefundDestination.WALLET_INSTANT)
                .reason(reason)
                .requestedBy("SYSTEM_BILLING_MODULE")
                .build();

        return transactionCore.processRefund(request);
    }
}
