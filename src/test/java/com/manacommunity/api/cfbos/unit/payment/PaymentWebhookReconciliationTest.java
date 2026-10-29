package com.manacommunity.api.cfbos.unit.payment;

import com.manacommunity.api.cfbos.integrations.service.PaymentWebhookReconciliationService;
import com.manacommunity.api.cfbos.integrations.service.PaymentWebhookReconciliationService.GatewaySettlementItem;
import com.manacommunity.api.cfbos.integrations.service.PaymentWebhookReconciliationService.CfbosReconciliationReport;
import com.manacommunity.api.cfbos.payment.entity.CfbosPayment;
import com.manacommunity.api.cfbos.payment.enums.PaymentStatus;
import com.manacommunity.api.cfbos.payment.repository.CfbosPaymentRepository;
import com.manacommunity.api.transactioncore.repository.TransactionIntentRepository;
import com.manacommunity.api.transactioncore.service.ManaTransactionCoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookReconciliationTest {

    @Mock
    private CfbosPaymentRepository paymentRepository;

    @Mock
    private TransactionIntentRepository intentRepository;

    @Mock
    private ManaTransactionCoreService transactionCoreService;

    private PaymentWebhookReconciliationService service;

    @BeforeEach
    void setUp() {
        service = new PaymentWebhookReconciliationService(paymentRepository, intentRepository, transactionCoreService);
    }

    @Test
    @DisplayName("Duplicate callbacks: Multiple webhook replays with same ID are processed idempotently")
    void testDuplicateCallbacks_processedIdempotently() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", "evt_razorpay_duplicate_123");
        payload.put("event", "payment.captured");

        boolean first = service.processRazorpayWebhook(payload, "sig1");
        assertThat(first).isTrue();
        assertThat(service.isEventProcessed("evt_razorpay_duplicate_123")).isTrue();

        // Duplicate replay
        boolean duplicate = service.processRazorpayWebhook(payload, "sig1");
        assertThat(duplicate).isTrue();
    }

    @Test
    @DisplayName("Concurrent duplicate callbacks: Parallel execution guarantees exactly 1 lock winner")
    void testConcurrentDuplicateCallbacks_threadSafety() throws Exception {
        String eventId = "evt_parallel_999";
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        AtomicInteger lockAcquiredCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    barrier.await();
                    if (service.tryAcquireLock(eventId)) {
                        lockAcquiredCount.incrementAndGet();
                        service.markEventProcessed(eventId);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        assertThat(lockAcquiredCount.get()).isEqualTo(1);
        assertThat(service.isEventProcessed(eventId)).isTrue();
    }

    @Test
    @DisplayName("Retries: Transient gateway error succeeds on subsequent attempt")
    void testGatewayRetries_recoversAfterTransientFailures() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);

        String result = service.executeWithRetry("QueryGatewayStatus", 3, 10, () -> {
            int cur = attempts.incrementAndGet();
            if (cur < 2) {
                throw new java.io.IOException("502 Bad Gateway");
            }
            return "GATEWAY_STATUS_PAID";
        });

        assertThat(result).isEqualTo("GATEWAY_STATUS_PAID");
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("Reconciliation: Accurately detects MATCHED, AMOUNT_MISMATCH, STATUS_MISMATCH, and MISSING_INTERNAL")
    void testReconciliation_discrepancyDetection() {
        CfbosPayment pMatched = CfbosPayment.builder()
                .id(1L)
                .gatewayReference("GW-REF-100")
                .amount(new BigDecimal("1000.00"))
                .status(PaymentStatus.SUCCESS)
                .build();

        CfbosPayment pAmtMismatch = CfbosPayment.builder()
                .id(2L)
                .gatewayReference("GW-REF-200")
                .amount(new BigDecimal("900.00"))
                .status(PaymentStatus.SUCCESS)
                .build();

        CfbosPayment pStatusMismatch = CfbosPayment.builder()
                .id(3L)
                .gatewayReference("GW-REF-300")
                .amount(new BigDecimal("500.00"))
                .status(PaymentStatus.FAILED)
                .build();

        when(paymentRepository.findByGatewayReference("GW-REF-100")).thenReturn(Optional.of(pMatched));
        when(paymentRepository.findByGatewayReference("GW-REF-200")).thenReturn(Optional.of(pAmtMismatch));
        when(paymentRepository.findByGatewayReference("GW-REF-300")).thenReturn(Optional.of(pStatusMismatch));
        when(paymentRepository.findByGatewayReference("GW-REF-400")).thenReturn(Optional.empty());

        List<GatewaySettlementItem> items = List.of(
                GatewaySettlementItem.builder().gatewayReference("GW-REF-100").amount(new BigDecimal("1000.00")).status("captured").build(),
                GatewaySettlementItem.builder().gatewayReference("GW-REF-200").amount(new BigDecimal("1000.00")).status("captured").build(),
                GatewaySettlementItem.builder().gatewayReference("GW-REF-300").amount(new BigDecimal("500.00")).status("captured").build(),
                GatewaySettlementItem.builder().gatewayReference("GW-REF-400").amount(new BigDecimal("250.00")).status("captured").build()
        );

        CfbosReconciliationReport report = service.reconcilePayments(items);

        assertThat(report.getTotalRecords()).isEqualTo(4);
        assertThat(report.getMatchedCount()).isEqualTo(1);
        assertThat(report.getAmountMismatchCount()).isEqualTo(1);
        assertThat(report.getStatusMismatchCount()).isEqualTo(1);
        assertThat(report.getMissingInternalCount()).isEqualTo(1);
        assertThat(report.getDiscrepancies()).hasSize(3);
    }
}
