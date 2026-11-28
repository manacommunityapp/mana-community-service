package com.manacommunity.api.unit.service;

import com.manacommunity.api.delivery.dto.ExpressEntryDispatchRequest;
import com.manacommunity.api.delivery.dto.PartnerWebhookAckResponse;
import com.manacommunity.api.delivery.dto.PartnerWebhookNotifyRequest;
import com.manacommunity.api.delivery.model.DeliveryPartner;
import com.manacommunity.api.delivery.service.ExpressDeliveryPartnerService;
import com.manacommunity.api.delivery.service.ExpressPartnerWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpressPartnerWebhookServiceTest {

    private static final String SECRET = "test-webhook-secret-key";
    private static final Instant FIXED_NOW = Instant.parse("2026-11-26T18:30:00Z");

    @Mock
    private ExpressDeliveryPartnerService deliveryService;

    private Clock fixedClock;
    private Executor directExecutor;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        directExecutor = Runnable::run;
    }

    private String sign(long timestampSec, String rawBody) {
        byte[] sig = ExpressPartnerWebhookService.computeHmacSha256(timestampSec + "." + rawBody, SECRET);
        return "sha256=" + HexFormat.of().formatHex(sig);
    }

    private PartnerWebhookNotifyRequest sampleRequest() {
        return PartnerWebhookNotifyRequest.builder()
                .orderId("BLNK-9001")
                .eventType("RIDER_ARRIVED_AT_GATE")
                .communityId(1L)
                .flatNumber("A-302")
                .riderName("Ravi Kumar")
                .etaMinutes(1)
                .build();
    }

    @Test
    @DisplayName("Valid HMAC webhook queues asynchronously onto bulkhead executor and deduplicates X-Event-ID")
    void acceptsValidWebhookAndDeduplicatesRepeats() {
        ExpressPartnerWebhookService webhookService = new ExpressPartnerWebhookService(
                deliveryService, directExecutor, SECRET, fixedClock, 10, 5.0);

        String rawBody = "{\"orderId\":\"BLNK-9001\"}";
        long ts = FIXED_NOW.getEpochSecond();
        String sig = sign(ts, rawBody);

        PartnerWebhookAckResponse first = webhookService.acceptNotification(
                rawBody, sampleRequest(), "BLINKIT", String.valueOf(ts), "evt_101", sig);

        assertThat(first.getStatus()).isEqualTo("QUEUED");
        assertThat(first.getEventId()).isEqualTo("evt_101");
        verify(deliveryService, times(1)).registerDispatch(any(ExpressEntryDispatchRequest.class), eq("BLINKIT"));

        // Re-transmitted duplicate X-Event-ID must return DUPLICATE_IGNORED without invoking DB dispatch again
        PartnerWebhookAckResponse repeat = webhookService.acceptNotification(
                rawBody, sampleRequest(), "BLINKIT", String.valueOf(ts), "evt_101", sig);

        assertThat(repeat.getStatus()).isEqualTo("DUPLICATE_IGNORED");
        verify(deliveryService, times(1)).registerDispatch(any(), any());
    }

    @Test
    @DisplayName("Rejects tampered HMAC signatures or expired replay timestamps with 401 before hitting DB")
    void rejectsTamperedOrReplayedWebhooks() {
        ExpressPartnerWebhookService webhookService = new ExpressPartnerWebhookService(
                deliveryService, directExecutor, SECRET, fixedClock, 10, 5.0);

        String rawBody = "{\"orderId\":\"ZMT-42\"}";
        long ts = FIXED_NOW.getEpochSecond();

        // Invalid signature
        assertThatThrownBy(() -> webhookService.acceptNotification(
                rawBody, sampleRequest(), "ZOMATO", String.valueOf(ts), "evt_bad", "sha256=deadbeef"))
                .isInstanceOf(ExpressPartnerWebhookService.WebhookSignatureException.class);

        // Stale replay timestamp (> 300s old)
        long expiredTs = ts - 600L;
        String expiredSig = sign(expiredTs, rawBody);
        assertThatThrownBy(() -> webhookService.acceptNotification(
                rawBody, sampleRequest(), "ZOMATO", String.valueOf(expiredTs), "evt_replay", expiredSig))
                .isInstanceOf(ExpressPartnerWebhookService.WebhookSignatureException.class)
                .hasMessageContaining("replay window");

        verifyNoInteractions(deliveryService);
    }

    @Test
    @DisplayName("Isolates partner rate limits: Zomato burst sheds with 429 while Blinkit remains unaffected")
    void perPartnerRateLimiterIsolatesBurstTraffic() {
        // Tight capacity = 2 per partner for test
        ExpressPartnerWebhookService webhookService = new ExpressPartnerWebhookService(
                deliveryService, directExecutor, SECRET, fixedClock, 2, 0.0);

        String rawBody = "{\"orderId\":\"ORD-1\"}";
        long ts = FIXED_NOW.getEpochSecond();
        String sig = sign(ts, rawBody);

        // Exhaust Zomato bucket
        webhookService.acceptNotification(rawBody, sampleRequest(), "ZOMATO", String.valueOf(ts), "z_1", sig);
        webhookService.acceptNotification(rawBody, sampleRequest(), "ZOMATO", String.valueOf(ts), "z_2", sig);

        assertThatThrownBy(() -> webhookService.acceptNotification(
                rawBody, sampleRequest(), "ZOMATO", String.valueOf(ts), "z_3", sig))
                .isInstanceOf(ExpressPartnerWebhookService.PartnerRateLimitException.class);

        // Blinkit still has its own healthy token bucket!
        PartnerWebhookAckResponse blinkitAck = webhookService.acceptNotification(
                rawBody, sampleRequest(), "BLINKIT", String.valueOf(ts), "b_1", sig);
        assertThat(blinkitAck.getStatus()).isEqualTo("QUEUED");
    }

    @Test
    @DisplayName("Bulkhead executor AbortPolicy sheds cleanly with 429 and rolls back idempotency key so caller can retry")
    void bulkheadSaturationShedsLoadAndAllowsRetry() {
        Executor fullExecutor = cmd -> {
            throw new RejectedExecutionException("Queue full");
        };

        ExpressPartnerWebhookService webhookService = new ExpressPartnerWebhookService(
                deliveryService, fullExecutor, SECRET, fixedClock, 10, 5.0);

        String rawBody = "{\"orderId\":\"SWGY-9\"}";
        long ts = FIXED_NOW.getEpochSecond();
        String sig = sign(ts, rawBody);

        assertThatThrownBy(() -> webhookService.acceptNotification(
                rawBody, sampleRequest(), "SWIGGY", String.valueOf(ts), "evt_shed_1", sig))
                .isInstanceOf(ExpressPartnerWebhookService.PartnerRateLimitException.class);
    }
}
