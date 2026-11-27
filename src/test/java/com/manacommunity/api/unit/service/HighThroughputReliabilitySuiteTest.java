package com.manacommunity.api.unit.service;

import com.manacommunity.api.notification.storm.NotificationStormThrottleService;
import com.manacommunity.api.notification.storm.NotificationStormThrottleService.PriorityLane;
import com.manacommunity.api.resilience.GatewayCircuitBreaker;
import com.manacommunity.api.resilience.GatewayFailoverRouter;
import com.manacommunity.api.visitor.entity.WhitelistEntry;
import com.manacommunity.api.visitor.repository.WhitelistRepository;
import com.manacommunity.api.visitor.search.GateFuzzySearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class HighThroughputReliabilitySuiteTest {

    @Mock
    private WhitelistRepository whitelistRepository;

    @Test
    @DisplayName("Notification storm preserves 30% critical headroom for live visitor approvals during 10k bulk blast")
    void notificationStormReservesCriticalHeadroom() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-11-27T01:30:00Z"), ZoneOffset.UTC);
        Executor direct = Runnable::run;

        // 10 TPS cap -> 3 tokens (30%) strictly reserved for CRITICAL_REALTIME
        NotificationStormThrottleService service =
                new NotificationStormThrottleService(direct, direct, fixedClock, 10);

        List<Long> tenThousandResidents = LongStream.rangeClosed(1, 10_000).boxed().toList();
        AtomicInteger batchesDispatched = new AtomicInteger(0);

        var plan = service.scheduleBulkBroadcast(tenThousandResidents, batch -> batchesDispatched.incrementAndGet());

        // 10,000 residents -> 20 chunks of 500 tokens each
        assertThat(plan.getTotalRecipients()).isEqualTo(10_000);
        assertThat(plan.getTotalChunks()).isEqualTo(20);
        // Only 7 chunks fit above the 3-token reserved floor; remaining 13 chunks paced
        assertThat(plan.getDispatchedChunks()).isEqualTo(7);
        assertThat(plan.getPacedChunks()).isEqualTo(13);
        assertThat(batchesDispatched.get()).isEqualTo(7);

        // Live visitor approvals CAN still acquire all 3 reserved critical tokens!
        assertThat(service.tryAcquireToken(PriorityLane.CRITICAL_REALTIME)).isTrue();
        assertThat(service.tryAcquireToken(PriorityLane.CRITICAL_REALTIME)).isTrue();
        assertThat(service.tryAcquireToken(PriorityLane.CRITICAL_REALTIME)).isTrue();
    }

    @Test
    @DisplayName("GateFuzzySearchService resolves last-4 plate digits, partial phone, and misspelled maid names cleanly")
    void fuzzySearchResolvesPlatesPhonesAndMisspelledNames() {
        GateFuzzySearchService searchService = new GateFuzzySearchService(whitelistRepository);

        List<WhitelistEntry> entries = List.of(
                WhitelistEntry.builder()
                        .id(1L).name("Sunita Devi").category("MAID")
                        .phone("+919876543210").vehicleNumber("KA-01-MJ-1234").isActive(true).build(),
                WhitelistEntry.builder()
                        .id(2L).name("Ramesh Driver").category("DRIVER")
                        .phone("+919988776655").vehicleNumber("MH-02-AB-9988").isActive(true).build(),
                WhitelistEntry.builder()
                        .id(3L).name("Pooja Cook").category("COOK")
                        .phone("+919112233445").isActive(true).build()
        );

        // 1. Last 4 digits of license plate
        var plateHits = searchService.rankMatches(entries, "1234", 5);
        assertThat(plateHits).hasSize(1);
        assertThat(plateHits.get(0).getName()).isEqualTo("Sunita Devi");
        assertThat(plateHits.get(0).getMatchedOn()).isEqualTo("PLATE_SUFFIX");

        // 2. Partial phone digits (phone returned masked for PII protection)
        var phoneHits = searchService.rankMatches(entries, "776655", 5);
        assertThat(phoneHits).hasSize(1);
        assertThat(phoneHits.get(0).getName()).isEqualTo("Ramesh Driver");
        assertThat(phoneHits.get(0).getMaskedPhone()).contains("****");

        // 3. Misspelled daily help name ("Suneeta" -> "Sunita Devi")
        var fuzzyNameHits = searchService.rankMatches(entries, "Suneeta", 5);
        assertThat(fuzzyNameHits).isNotEmpty();
        assertThat(fuzzyNameHits.get(0).getName()).isEqualTo("Sunita Devi");
        assertThat(fuzzyNameHits.get(0).getMatchedOn()).isEqualTo("NAME_FUZZY");

        // 4. Verify ES mapping spec includes custom suffix/phone/name analyzers
        var spec = searchService.getElasticsearchMappingSpec();
        assertThat(spec).containsKey("settings");
    }

    @Test
    @DisplayName("GatewayCircuitBreaker trips on 5xx threshold, ignores 4xx business errors, and recovers via HALF_OPEN")
    void circuitBreakerLifecycleAndClientErrorExclusion() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-27T01:30:00Z"));
        GatewayCircuitBreaker breaker = new GatewayCircuitBreaker(
                "RAZORPAY", 4, 50.0, Duration.ofSeconds(30), 2, clock);

        // 4xx Client errors must NEVER trip the breaker
        for (int i = 0; i < 6; i++) {
            assertThatThrownBy(() -> breaker.execute(() -> {
                throw new GatewayCircuitBreaker.ClientBusinessException("Invalid UPI VPA");
            })).isInstanceOf(GatewayCircuitBreaker.ClientBusinessException.class);
        }
        assertThat(breaker.getState()).isEqualTo(GatewayCircuitBreaker.State.CLOSED);

        // Trigger 4 upstream 5xx errors over sliding window of 4 -> trips to OPEN
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> breaker.execute(() -> {
                throw new RuntimeException("502 Bad Gateway");
            })).isInstanceOf(RuntimeException.class);
        }
        assertThat(breaker.getState()).isEqualTo(GatewayCircuitBreaker.State.OPEN);

        // Subsequent call fails fast with CircuitOpenException
        assertThatThrownBy(() -> breaker.execute(() -> "ok"))
                .isInstanceOf(GatewayCircuitBreaker.CircuitOpenException.class);

        // Advance clock past 30s cooldown -> HALF_OPEN allows 2 canary probes and restores CLOSED
        clock.advanceSeconds(31);
        assertThat(breaker.getState()).isEqualTo(GatewayCircuitBreaker.State.HALF_OPEN);
        assertThat(breaker.execute(() -> "probe-1")).isEqualTo("probe-1");
        assertThat(breaker.execute(() -> "probe-2")).isEqualTo("probe-2");
        assertThat(breaker.getState()).isEqualTo(GatewayCircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("GatewayFailoverRouter routes Razorpay 5xx to Cashfree and degrades cleanly when all providers fail")
    void gatewayFailoverRouterSeamlessSwitchAndDegradation() {
        GatewayFailoverRouter router = new GatewayFailoverRouter();

        // Scenario 1: Razorpay 5xx -> Cashfree succeeds
        var failoverRes = router.executePaymentWithFailover(
                "RAZORPAY",
                () -> { throw new RuntimeException("Razorpay 503 Unavailable"); },
                "CASHFREE",
                () -> "cf_order_9988"
        );
        assertThat(failoverRes.isFailoverTriggered()).isTrue();
        assertThat(failoverRes.isDegradedMode()).isFalse();
        assertThat(failoverRes.getProviderUsed()).isEqualTo("CASHFREE");
        assertThat(failoverRes.getPayload()).isEqualTo("cf_order_9988");

        // Scenario 2: Both Primary & Secondary IVR providers fail -> Graceful Push/SMS fallback
        var ivrDegraded = router.executeIvrWithFailover(
                () -> { throw new RuntimeException("Telco-1 timeout"); },
                () -> { throw new RuntimeException("Telco-2 500"); }
        );
        assertThat(ivrDegraded.isDegradedMode()).isTrue();
        assertThat(ivrDegraded.getProviderUsed()).isEqualTo("PUSH_SMS_OTP_FALLBACK");
        assertThat(ivrDegraded.getDegradationReason()).contains("App Push + SMS OTP");
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant initial) {
            this.instant = initial;
        }

        void advanceSeconds(long secs) {
            this.instant = this.instant.plusSeconds(secs);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
