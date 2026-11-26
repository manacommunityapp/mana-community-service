package com.manacommunity.api.delivery.service;

import com.manacommunity.api.delivery.dto.ExpressEntryDispatchRequest;
import com.manacommunity.api.delivery.dto.PartnerWebhookAckResponse;
import com.manacommunity.api.delivery.dto.PartnerWebhookNotifyRequest;
import com.manacommunity.api.delivery.model.DeliveryPartner;
import com.manacommunity.api.exception.ManaCommunityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * High-throughput, bulkhead-isolated ingestion service for third-party quick-commerce
 * and food delivery webhooks (Blinkit, Zomato, Swiggy, Zepto).
 *
 * <ul>
 *   <li>Verifies HMAC-SHA256 signature and 300s replay timestamp window before touching queues or DB.</li>
 *   <li>Enforces per-partner Token Bucket rate limits so retry storms shed cleanly via HTTP 429.</li>
 *   <li>Deduplicates X-Event-ID and returns HTTP 202 Accepted in &lt;10ms on an isolated executor.</li>
 * </ul>
 */
@Slf4j
@Service
public class ExpressPartnerWebhookService {

    private static final long MAX_TIMESTAMP_DRIFT_SECONDS = 300L;
    private static final int MAX_IDEMPOTENCY_CACHE_SIZE = 50_000;
    private static final int DEFAULT_BUCKET_CAPACITY = 120;
    private static final double DEFAULT_REFILL_PER_SECOND = 60.0;

    private final ExpressDeliveryPartnerService deliveryService;
    private final Executor partnerWebhookExecutor;
    private final String webhookSecret;
    private final Clock clock;
    private final int bucketCapacity;
    private final double refillPerSecond;

    private final ConcurrentHashMap<String, TokenBucket> partnerBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> processedEvents = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public ExpressPartnerWebhookService(
            ExpressDeliveryPartnerService deliveryService,
            @Qualifier("partnerWebhookExecutor") Executor partnerWebhookExecutor,
            @Value("${app.delivery.webhook.hmac-secret:mana-partner-webhook-dev-secret}") String webhookSecret) {
        this(deliveryService, partnerWebhookExecutor, webhookSecret, Clock.systemUTC(),
                DEFAULT_BUCKET_CAPACITY, DEFAULT_REFILL_PER_SECOND);
    }

    public ExpressPartnerWebhookService(
            ExpressDeliveryPartnerService deliveryService,
            Executor partnerWebhookExecutor,
            String webhookSecret,
            Clock clock,
            int bucketCapacity,
            double refillPerSecond) {
        this.deliveryService = deliveryService;
        this.partnerWebhookExecutor = partnerWebhookExecutor;
        this.webhookSecret = webhookSecret;
        this.clock = clock;
        this.bucketCapacity = bucketCapacity;
        this.refillPerSecond = refillPerSecond;
    }

    public PartnerWebhookAckResponse acceptNotification(
            String rawPayload,
            PartnerWebhookNotifyRequest request,
            String partnerId,
            String timestampHeader,
            String eventId,
            String signatureHeader) {

        String normalizedPartner = (partnerId != null && !partnerId.isBlank())
                ? partnerId.trim().toUpperCase(Locale.ROOT)
                : (request.getPartner() != null ? request.getPartner().name() : "OTHER");

        // 1. Edge Security: HMAC-SHA256 + 300s Replay Window (Zero DB hits)
        verifySignatureAndReplayWindow(rawPayload, timestampHeader, signatureHeader, normalizedPartner);

        // 2. Per-Partner Token Bucket Rate Limiting (protects resident traffic from bursty retries)
        TokenBucket bucket = partnerBuckets.computeIfAbsent(
                normalizedPartner,
                k -> new TokenBucket(bucketCapacity, refillPerSecond, clock));
        if (!bucket.tryConsume()) {
            log.warn("Rate-limited partner webhook traffic partner={} eventId={}", normalizedPartner, eventId);
            throw new PartnerRateLimitException("Rate limit exceeded for delivery partner: " + normalizedPartner);
        }

        // 3. Idempotent X-Event-ID deduplication
        String effectiveEventId = (eventId != null && !eventId.isBlank())
                ? eventId.trim()
                : normalizedPartner + ":" + request.getOrderId() + ":" + request.getEventType();

        evictIdempotencyIfOversized();
        Instant now = Instant.now(clock);
        if (processedEvents.putIfAbsent(effectiveEventId, now) != null) {
            return PartnerWebhookAckResponse.builder()
                    .eventId(effectiveEventId)
                    .partner(normalizedPartner)
                    .orderId(request.getOrderId())
                    .status("DUPLICATE_IGNORED")
                    .acceptedAt(now)
                    .message("Webhook event already accepted")
                    .build();
        }

        // 4. Hand off to isolated bulkhead executor; reject cleanly with 429 if saturated
        try {
            partnerWebhookExecutor.execute(() -> processAsync(request, normalizedPartner, effectiveEventId));
        } catch (RejectedExecutionException ex) {
            processedEvents.remove(effectiveEventId);
            log.warn("Partner webhook bulkhead saturated — shedding eventId={} partner={}",
                    effectiveEventId, normalizedPartner);
            throw new PartnerRateLimitException("Partner ingestion buffer full. Retry after backoff.");
        }

        return PartnerWebhookAckResponse.builder()
                .eventId(effectiveEventId)
                .partner(normalizedPartner)
                .orderId(request.getOrderId())
                .status("QUEUED")
                .acceptedAt(now)
                .message("Accepted for asynchronous gate processing")
                .build();
    }

    private void processAsync(PartnerWebhookNotifyRequest req, String partnerId, String eventId) {
        try {
            DeliveryPartner resolvedPartner = req.getPartner() != null
                    ? req.getPartner()
                    : parsePartner(partnerId);

            ExpressEntryDispatchRequest dispatch = ExpressEntryDispatchRequest.builder()
                    .partner(resolvedPartner)
                    .orderId(req.getOrderId())
                    .communityId(req.getCommunityId())
                    .flatNumber(req.getFlatNumber())
                    .residentName(req.getResidentName())
                    .riderName(req.getRiderName())
                    .riderPhone(req.getRiderPhone())
                    .vehicleNumber(req.getVehicleNumber())
                    .etaMinutes(req.getEtaMinutes())
                    .itemsSummary(req.getItemsSummary() != null ? req.getItemsSummary() : req.getEventType())
                    .build();

            deliveryService.registerDispatch(dispatch, partnerId);
            log.debug("Processed partner webhook asynchronously eventId={} orderId={}", eventId, req.getOrderId());
        } catch (Exception ex) {
            log.error("Failed async processing for partner webhook eventId={} orderId={}: {}",
                    eventId, req.getOrderId(), ex.getMessage(), ex);
        }
    }

    private void verifySignatureAndReplayWindow(
            String rawPayload,
            String timestampHeader,
            String signatureHeader,
            String partnerId) {

        if (timestampHeader == null || timestampHeader.isBlank()
                || signatureHeader == null || signatureHeader.isBlank()) {
            throw new WebhookSignatureException("Missing X-Timestamp or X-Signature header for " + partnerId);
        }

        long timestampSec;
        try {
            timestampSec = Long.parseLong(timestampHeader.trim());
        } catch (NumberFormatException ex) {
            throw new WebhookSignatureException("Invalid X-Timestamp header format");
        }

        long nowSec = Instant.now(clock).getEpochSecond();
        if (Math.abs(nowSec - timestampSec) > MAX_TIMESTAMP_DRIFT_SECONDS) {
            throw new WebhookSignatureException("Webhook timestamp outside permitted replay window");
        }

        String normalizedSig = signatureHeader.trim().toLowerCase(Locale.ROOT);
        if (normalizedSig.startsWith("sha256=")) {
            normalizedSig = normalizedSig.substring("sha256=".length());
        }

        byte[] provided;
        try {
            provided = HexFormat.of().parseHex(normalizedSig);
        } catch (IllegalArgumentException ex) {
            throw new WebhookSignatureException("Malformed HMAC signature encoding");
        }

        String signedInput = timestampHeader.trim() + "." + (rawPayload != null ? rawPayload : "");
        byte[] expected = computeHmacSha256(signedInput, webhookSecret);
        if (!MessageDigest.isEqual(expected, provided)) {
            throw new WebhookSignatureException("HMAC webhook signature verification failed for " + partnerId);
        }
    }

    public static byte[] computeHmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute HmacSHA256", e);
        }
    }

    private void evictIdempotencyIfOversized() {
        if (processedEvents.size() < MAX_IDEMPOTENCY_CACHE_SIZE) return;
        Instant cutoff = Instant.now(clock).minusSeconds(86_400L);
        processedEvents.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
    }

    private DeliveryPartner parsePartner(String partnerId) {
        try {
            return DeliveryPartner.valueOf(partnerId.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return DeliveryPartner.OTHER;
        }
    }

    /** Thread-safe Token Bucket limiter per delivery partner. */
    private static final class TokenBucket {
        private final int capacity;
        private final double refillPerSecond;
        private final Clock clock;
        private double tokens;
        private long lastRefillMillis;

        TokenBucket(int capacity, double refillPerSecond, Clock clock) {
            this.capacity = capacity;
            this.refillPerSecond = refillPerSecond;
            this.clock = clock;
            this.tokens = capacity;
            this.lastRefillMillis = clock.millis();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = clock.millis();
            long elapsedMs = Math.max(0L, now - lastRefillMillis);
            if (elapsedMs > 0) {
                tokens = Math.min(capacity, tokens + (elapsedMs / 1000.0) * refillPerSecond);
                lastRefillMillis = now;
            }
        }
    }

    public static class WebhookSignatureException extends ManaCommunityException {
        public WebhookSignatureException(String message) {
            super(message, HttpStatus.UNAUTHORIZED, "INVALID_WEBHOOK_SIGNATURE");
        }
    }

    public static class PartnerRateLimitException extends ManaCommunityException {
        public PartnerRateLimitException(String message) {
            super(message, HttpStatus.TOO_MANY_REQUESTS, "PARTNER_RATE_LIMITED");
        }
    }
}
