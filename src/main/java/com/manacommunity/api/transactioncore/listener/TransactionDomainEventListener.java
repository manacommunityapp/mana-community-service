package com.manacommunity.api.transactioncore.listener;

import com.manacommunity.api.cfbos.invoice.entity.CfbosInvoice;
import com.manacommunity.api.cfbos.invoice.repository.CfbosInvoiceRepository;
import com.manacommunity.api.cfbos.payment.dto.RecordPaymentRequest;
import com.manacommunity.api.cfbos.payment.engine.PaymentEngine;
import com.manacommunity.api.cfbos.payment.enums.PaymentMethodType;
import com.manacommunity.api.transactioncore.enums.TransactionDomain;
import com.manacommunity.api.transactioncore.event.TransactionCapturedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionDomainEventListener {

    private final CfbosInvoiceRepository invoiceRepository;
    private final PaymentEngine paymentEngine;

    @EventListener
    public void onTransactionCaptured(TransactionCapturedEvent event) {
        log.info("[DomainEvent] Transaction captured: intentId={} domain={} orderRef={} amount={}",
                event.getIntentId(), event.getDomain(), event.getOrderReferenceId(), event.getAmount());

        if (event.getDomain() == TransactionDomain.COMMUNITY_BILLING ||
            event.getDomain() == TransactionDomain.MAINTENANCE_BILL) {
            handleCommunityBillingCapture(event);
        } else if (event.getDomain() == TransactionDomain.RESOURCE_BOOKING) {
            handleResourceBookingCapture(event);
        } else if (event.getDomain() == TransactionDomain.EVENTS ||
                   event.getDomain() == TransactionDomain.POOJA_BOOKING) {
            handleEventBookingCapture(event);
        } else {
            log.info("[DomainEvent] Generic domain fulfillment captured for {}", event.getDomain());
        }
    }

    private void handleCommunityBillingCapture(TransactionCapturedEvent event) {
        try {
            Optional<CfbosInvoice> invoiceOpt = invoiceRepository.findByInvoiceNumber(event.getOrderReferenceId());
            if (invoiceOpt.isPresent()) {
                CfbosInvoice invoice = invoiceOpt.get();
                log.info("[DomainEvent] Matching CFBOS Invoice found for {}: id={}, status={}",
                        event.getOrderReferenceId(), invoice.getId(), invoice.getStatus());

                RecordPaymentRequest paymentRequest = RecordPaymentRequest.builder()
                        .residentId(invoice.getResidentId())
                        .propertyId(invoice.getPropertyId() != null ? invoice.getPropertyId() : 0L)
                        .amount(event.getAmount())
                        .paymentMethod(PaymentMethodType.UPI)
                        .paymentMode("ONLINE")
                        .paymentDate(LocalDate.now())
                        .gatewayReference(event.getGatewayPaymentId() != null ? event.getGatewayPaymentId() : event.getUtr())
                        .remarks("Mana Pay: " + event.getIntentId() + (event.getUtr() != null ? " UTR: " + event.getUtr() : ""))
                        .specificInvoiceIds(List.of(invoice.getId()))
                        .build();

                paymentEngine.processPayment(paymentRequest);
                log.info("[DomainEvent] Successfully applied payment to CFBOS Invoice {}", invoice.getInvoiceNumber());
            } else {
                log.info("[DomainEvent] No matching CFBOS Invoice found for {}. Standalone community receipt preserved.",
                        event.getOrderReferenceId());
            }
        } catch (Exception e) {
            log.error("[DomainEvent] Failed to reconcile CFBOS billing for intent {}: {}", event.getIntentId(), e.getMessage(), e);
        }
    }

    private void handleResourceBookingCapture(TransactionCapturedEvent event) {
        log.info("[DomainEvent] Resource booking payment captured for bookingRef={}", event.getOrderReferenceId());
    }

    private void handleEventBookingCapture(TransactionCapturedEvent event) {
        log.info("[DomainEvent] Event / Pooja booking payment captured for ref={}", event.getOrderReferenceId());
    }
}
