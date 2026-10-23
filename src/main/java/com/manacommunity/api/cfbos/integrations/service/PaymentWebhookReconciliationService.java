package com.manacommunity.api.cfbos.integrations.service;

import com.manacommunity.api.cfbos.payment.entity.CfbosPayment;
import com.manacommunity.api.cfbos.payment.enums.PaymentStatus;
import com.manacommunity.api.cfbos.payment.repository.CfbosPaymentRepository;
import com.manacommunity.api.transactioncore.dto.ConfirmPaymentRequest;
import com.manacommunity.api.transactioncore.entity.TransactionIntent;
import com.manacommunity.api.transactioncore.enums.PaymentRail;
import com.manacommunity.api.transactioncore.repository.TransactionIntentRepository;
import com.manacommunity.api.transactioncore.service.ManaTransactionCoreService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentWebhookReconciliationService {

    private final CfbosPaymentRepository paymentRepository;
    private final TransactionIntentRepository intentRepository;
    private final ManaTransactionCoreService transactionCoreService;

    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();
    private final Map<String, Object> inFlightLocks = new ConcurrentHashMap<>();

    public boolean isEventProcessed(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            return false;
        }
        return processedEventIds.contains(eventId);
    }

    public boolean tryAcquireLock(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            return true;
        }
        if (processedEventIds.contains(eventId)) {
            return false;
        }
        return inFlightLocks.putIfAbsent(eventId, new Object()) == null;
    }

    public void markEventProcessed(String eventId) {
        if (eventId != null && !eventId.trim().isEmpty()) {
            processedEventIds.add(eventId);
            inFlightLocks.remove(eventId);
            log.info("[IDEMPOTENCY] Event {} recorded as processed", eventId);
        }
    }

    public boolean processRazorpayWebhook(Map<String, Object> payload, String signature) {
        String eventId = payload != null && payload.containsKey("id") ? String.valueOf(payload.get("id")) : null;
        log.info("Processing Razorpay webhook event: {}, eventId={}", payload != null ? payload.get("event") : null, eventId);

        if (eventId != null) {
            if (!tryAcquireLock(eventId)) {
                log.warn("[DUPLICATE] Duplicate Razorpay webhook ignored for eventId={}", eventId);
                return true;
            }
        }

        try {
            String eventName = payload != null && payload.containsKey("event") ? String.valueOf(payload.get("event")) : null;
            if ("payment.captured".equals(eventName) || "order.paid".equals(eventName)) {
                handleRazorpayPaymentCaptured(payload);
            }
            markEventProcessed(eventId);
            return true;
        } catch (Exception e) {
            log.error("Error processing Razorpay webhook event {}: {}", eventId, e.getMessage(), e);
            if (eventId != null) {
                inFlightLocks.remove(eventId);
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private void handleRazorpayPaymentCaptured(Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("payload")) {
            return;
        }

        Map<String, Object> payloadData = (Map<String, Object>) payload.get("payload");
        if (payloadData == null) return;

        Map<String, Object> paymentWrapper = (Map<String, Object>) payloadData.get("payment");
        Map<String, Object> paymentEntity = paymentWrapper != null ? (Map<String, Object>) paymentWrapper.get("entity") : null;

        Map<String, Object> orderWrapper = (Map<String, Object>) payloadData.get("order");
        Map<String, Object> orderEntity = orderWrapper != null ? (Map<String, Object>) orderWrapper.get("entity") : null;

        String paymentId = paymentEntity != null ? (String) paymentEntity.get("id") : null;
        String orderId = paymentEntity != null && paymentEntity.get("order_id") != null
                ? (String) paymentEntity.get("order_id")
                : (orderEntity != null ? (String) orderEntity.get("id") : null);
        String method = paymentEntity != null ? (String) paymentEntity.get("method") : null;

        String rrn = null;
        if (paymentEntity != null && paymentEntity.containsKey("acquirer_data") && paymentEntity.get("acquirer_data") instanceof Map) {
            Map<String, Object> acquirerData = (Map<String, Object>) paymentEntity.get("acquirer_data");
            if (acquirerData != null) {
                if (acquirerData.containsKey("rrn")) {
                    rrn = String.valueOf(acquirerData.get("rrn"));
                } else if (acquirerData.containsKey("upi_transaction_id")) {
                    rrn = String.valueOf(acquirerData.get("upi_transaction_id"));
                }
            }
        }

        String intentId = null;
        String orderRef = null;
        if (paymentEntity != null && paymentEntity.containsKey("notes") && paymentEntity.get("notes") instanceof Map) {
            Map<String, Object> notes = (Map<String, Object>) paymentEntity.get("notes");
            if (notes != null) {
                if (notes.containsKey("intentId")) intentId = String.valueOf(notes.get("intentId"));
                if (notes.containsKey("orderReferenceId")) orderRef = String.valueOf(notes.get("orderReferenceId"));
            }
        }

        PaymentRail rail = PaymentRail.UPI;
        if ("card".equalsIgnoreCase(method)) {
            rail = PaymentRail.CARD;
        } else if ("netbanking".equalsIgnoreCase(method)) {
            rail = PaymentRail.NET_BANKING;
        } else if ("wallet".equalsIgnoreCase(method)) {
            rail = PaymentRail.WALLET;
        } else if ("upi".equalsIgnoreCase(method)) {
            rail = PaymentRail.UPI_INTENT;
        }

        // Locate TransactionIntent
        Optional<TransactionIntent> intentOpt = Optional.empty();
        if (intentId != null && !intentId.isBlank()) {
            intentOpt = intentRepository.findByIntentId(intentId);
        }
        if (intentOpt.isEmpty() && orderId != null && !orderId.isBlank()) {
            intentOpt = intentRepository.findByGatewayOrderId(orderId);
        }
        if (intentOpt.isEmpty() && orderRef != null && !orderRef.isBlank()) {
            intentOpt = intentRepository.findByOrderReferenceId(orderRef);
        }

        if (intentOpt.isPresent()) {
            TransactionIntent intent = intentOpt.get();
            log.info("[WebhookMatch] Matched TransactionIntent {} for orderId={}, paymentId={}",
                    intent.getIntentId(), orderId, paymentId);

            transactionCoreService.confirmPayment(ConfirmPaymentRequest.builder()
                    .intentId(intent.getIntentId())
                    .gatewayPaymentId(paymentId)
                    .gatewayOrderId(orderId)
                    .utr(rrn)
                    .rail(rail)
                    .build());
        } else {
            log.info("[WebhookMatch] No TransactionIntent found for orderId={} paymentId={}. Checking CFBOS...",
                    orderId, paymentId);
        }
    }

    public boolean processStripeWebhook(String rawPayload, String sigHeader) {
        log.info("Processing Stripe webhook");
        return true;
    }

    public <T> T executeWithRetry(String operationName, int maxAttempts, long initialBackoffMs, Callable<T> callable) throws Exception {
        int attempt = 0;
        long currentBackoff = initialBackoffMs;
        Exception lastException = null;

        while (attempt < maxAttempts) {
            attempt++;
            try {
                return callable.call();
            } catch (Exception e) {
                lastException = e;
                log.warn("[RETRY] {} failed on attempt {}/{}: {}", operationName, attempt, maxAttempts, e.getMessage());
                if (attempt >= maxAttempts) {
                    break;
                }
                try {
                    Thread.sleep(currentBackoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ie;
                }
                currentBackoff *= 2;
            }
        }
        log.error("[RETRY EXHAUSTED] {} failed after {} attempts", operationName, maxAttempts);
        throw lastException != null ? lastException : new RuntimeException("Operation failed after retries: " + operationName);
    }

    public CfbosReconciliationReport reconcilePayments(List<GatewaySettlementItem> gatewayRecords) {
        String reportId = "REC-CFBOS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[RECONCILIATION] Reconciling {} gateway records for report {}", gatewayRecords.size(), reportId);

        int matched = 0;
        int amountMismatch = 0;
        int statusMismatch = 0;
        int missingInternal = 0;
        BigDecimal totalGw = BigDecimal.ZERO;
        BigDecimal totalInt = BigDecimal.ZERO;
        List<CfbosReconciliationReport.DiscrepancyItem> discrepancies = new ArrayList<>();

        for (GatewaySettlementItem item : gatewayRecords) {
            BigDecimal gwAmt = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
            totalGw = totalGw.add(gwAmt);

            Optional<CfbosPayment> paymentOpt = paymentRepository.findByGatewayReference(item.getGatewayReference());
            if (paymentOpt.isEmpty()) {
                missingInternal++;
                discrepancies.add(CfbosReconciliationReport.DiscrepancyItem.builder()
                        .gatewayReference(item.getGatewayReference())
                        .gatewayAmount(gwAmt)
                        .internalAmount(BigDecimal.ZERO)
                        .type("MISSING_INTERNAL")
                        .reason("Payment captured on gateway but missing in internal CFBOS ledger")
                        .build());
                continue;
            }

            CfbosPayment payment = paymentOpt.get();
            BigDecimal intAmt = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
            totalInt = totalInt.add(intAmt);

            boolean amountsMatch = gwAmt.compareTo(intAmt) == 0;
            boolean statusesMatch = "captured".equalsIgnoreCase(item.getStatus()) && payment.getStatus() == PaymentStatus.SUCCESS;

            if (amountsMatch && statusesMatch) {
                matched++;
            } else if (!amountsMatch) {
                amountMismatch++;
                discrepancies.add(CfbosReconciliationReport.DiscrepancyItem.builder()
                        .gatewayReference(item.getGatewayReference())
                        .gatewayAmount(gwAmt)
                        .internalAmount(intAmt)
                        .type("AMOUNT_MISMATCH")
                        .reason(String.format("Amount mismatch: Gateway %s vs Internal %s", gwAmt, intAmt))
                        .build());
            } else {
                statusMismatch++;
                discrepancies.add(CfbosReconciliationReport.DiscrepancyItem.builder()
                        .gatewayReference(item.getGatewayReference())
                        .gatewayAmount(gwAmt)
                        .internalAmount(intAmt)
                        .type("STATUS_MISMATCH")
                        .reason(String.format("Status mismatch: Gateway %s vs Internal %s", item.getStatus(), payment.getStatus()))
                        .build());
            }
        }

        return CfbosReconciliationReport.builder()
                .reportId(reportId)
                .totalRecords(gatewayRecords.size())
                .matchedCount(matched)
                .amountMismatchCount(amountMismatch)
                .statusMismatchCount(statusMismatch)
                .missingInternalCount(missingInternal)
                .totalGatewayAmount(totalGw)
                .totalInternalAmount(totalInt)
                .discrepancies(discrepancies)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GatewaySettlementItem {
        private String gatewayReference;
        private BigDecimal amount;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CfbosReconciliationReport {
        private String reportId;
        private int totalRecords;
        private int matchedCount;
        private int amountMismatchCount;
        private int statusMismatchCount;
        private int missingInternalCount;
        private BigDecimal totalGatewayAmount;
        private BigDecimal totalInternalAmount;
        @Builder.Default
        private List<DiscrepancyItem> discrepancies = new ArrayList<>();

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DiscrepancyItem {
            private String gatewayReference;
            private BigDecimal gatewayAmount;
            private BigDecimal internalAmount;
            private String type;
            private String reason;
        }
    }
}
