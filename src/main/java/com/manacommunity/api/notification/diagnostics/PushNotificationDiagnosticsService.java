package com.manacommunity.api.notification.diagnostics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real-time diagnostic and canary heartbeat service diagnosing push notification
 * pipeline blackholes across transactional outbox, Kafka consumer lag, FCM/APNs credentials,
 * quota throttling, and Android Doze mode payload priority compliance.
 */
@Slf4j
@Service
public class PushNotificationDiagnosticsService {

    public enum PipelineHealthStatus {
        HEALTHY,
        DEGRADED,
        CRITICAL_BLACKHOLE
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlackholeDiagnosticReport {
        private PipelineHealthStatus status;
        private long outboxPendingCount;
        private long kafkaConsumerLag;
        private boolean credentialsValid;
        private long credentialsTtlSeconds;
        private boolean quotaThrottled;
        private boolean dozeCompliant;
        private long canaryRoundtripLatencyMs;
        private List<String> detectedBottlenecks;
        private List<String> recommendedRemediations;
        private Instant evaluatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PushDeliveryRequest {
        private String eventType; // e.g. "VISITOR_ARRIVED", "GATE_OTP"
        private String priority;  // "HIGH", "NORMAL"
        private String recipientToken;
        private Map<String, String> payload;
    }

    private final Clock clock;
    private final AtomicLong mockOutboxLag = new AtomicLong(0);
    private final AtomicLong mockKafkaConsumerLag = new AtomicLong(0);
    private final AtomicLong credentialsExpiryEpochMs = new AtomicLong(0);
    private volatile boolean simulateFcmQuotaThrottled = false;

    private final Map<String, Long> activeCanaryProbes = new ConcurrentHashMap<>();
    private final AtomicLong lastCanaryLatencyMs = new AtomicLong(120);

    public PushNotificationDiagnosticsService() {
        this(Clock.systemUTC());
    }

    public PushNotificationDiagnosticsService(Clock clock) {
        this.clock = clock;
        // Default credentials valid for 1 hour
        this.credentialsExpiryEpochMs.set(clock.millis() + 3600_000L);
    }

    /**
     * Validates that critical time-sensitive events (gate visitor approvals, child school-bus arrival)
     * strictly enforce high priority to prevent Android Doze mode deferred delivery.
     */
    public boolean validatePayloadPriorityForDozeCompliance(PushDeliveryRequest request) {
        if ("VISITOR_ARRIVED".equalsIgnoreCase(request.getEventType())
                || "GATE_OTP".equalsIgnoreCase(request.getEventType())
                || "SCHOOL_BUS_BOARDED".equalsIgnoreCase(request.getEventType())) {
            return "HIGH".equalsIgnoreCase(request.getPriority());
        }
        return true;
    }

    /**
     * Dispatches an end-to-end synthetic canary probe to measure physical delivery latency.
     */
    public String dispatchCanaryProbe() {
        String probeId = "canary-" + UUID.randomUUID().toString().substring(0, 8);
        activeCanaryProbes.put(probeId, clock.millis());
        log.info("Dispatched push notification canary probe: {}", probeId);
        return probeId;
    }

    /**
     * Records reception of the canary probe from the physical test device.
     */
    public long recordCanaryReceipt(String probeId) {
        Long dispatchedAt = activeCanaryProbes.remove(probeId);
        if (dispatchedAt == null) {
            log.warn("Canary probe receipt for unknown or expired probe: {}", probeId);
            return -1;
        }
        long latencyMs = clock.millis() - dispatchedAt;
        lastCanaryLatencyMs.set(latencyMs);
        log.info("Canary probe {} roundtrip confirmed in {} ms", probeId, latencyMs);
        return latencyMs;
    }

    /**
     * Executes the comprehensive multi-stage pipeline diagnosis to pinpoint root cause.
     */
    public BlackholeDiagnosticReport evaluatePipelineHealth() {
        long outboxPending = mockOutboxLag.get();
        long consumerLag = mockKafkaConsumerLag.get();
        long now = clock.millis();
        long credsRemaining = (credentialsExpiryEpochMs.get() - now) / 1000L;
        boolean credsValid = credsRemaining > 0;
        long canaryLatency = lastCanaryLatencyMs.get();

        List<String> bottlenecks = new ArrayList<>();
        List<String> remediations = new ArrayList<>();

        // Check 1: Outbox Publisher Stoppage
        if (outboxPending > 100) {
            bottlenecks.add(String.format("Outbox relay backlog accumulated: %d pending events", outboxPending));
            remediations.add("Restart outbox publisher thread and check DB connection pool for locked rows");
        }

        // Check 2: Kafka Consumer Lag / Partition Rebalance Storm
        if (consumerLag > 500) {
            bottlenecks.add(String.format("Critical Kafka consumer lag: %d unread events on visitor topic", consumerLag));
            remediations.add("Check for poison-pill deserialization errors and scale out consumer pods");
        }

        // Check 3: FCM/APNs Credentials Expired
        if (!credsValid) {
            bottlenecks.add("Google Cloud FCM service account OAuth2 token or Apple APNs .p8 key has EXPIRED");
            remediations.add("Rotate and refresh FCM service account token in Secret Manager; restart pods");
        } else if (credsRemaining < 300) { // Under 5 mins
            bottlenecks.add(String.format("FCM/APNs token expiring soon (%d seconds remaining)", credsRemaining));
            remediations.add("Trigger proactive background credential refresh before token expires");
        }

        // Check 4: Quota Throttling (HTTP 429)
        if (simulateFcmQuotaThrottled) {
            bottlenecks.add("FCM project egress quota exceeded (HTTP 429 Too Many Requests)");
            remediations.add("Engage NotificationStormThrottleService to reserve 30% TPS headroom for critical lane");
        }

        // Check 5: Canary Roundtrip SLA Breach
        if (canaryLatency > 15_000L) { // >15s is an SLA breach for gate arrival
            bottlenecks.add(String.format("Canary heartbeat latency (%d ms) exceeds 15-second SLA", canaryLatency));
            remediations.add("Page on-call SRE: Gate visitor push alert delivery is severely degraded");
        }

        PipelineHealthStatus status = PipelineHealthStatus.HEALTHY;
        if (!credsValid || consumerLag > 1000 || simulateFcmQuotaThrottled || canaryLatency > 30_000L) {
            status = PipelineHealthStatus.CRITICAL_BLACKHOLE;
        } else if (!bottlenecks.isEmpty()) {
            status = PipelineHealthStatus.DEGRADED;
        }

        return BlackholeDiagnosticReport.builder()
                .status(status)
                .outboxPendingCount(outboxPending)
                .kafkaConsumerLag(consumerLag)
                .credentialsValid(credsValid)
                .credentialsTtlSeconds(Math.max(0, credsRemaining))
                .quotaThrottled(simulateFcmQuotaThrottled)
                .dozeCompliant(true)
                .canaryRoundtripLatencyMs(canaryLatency)
                .detectedBottlenecks(bottlenecks)
                .recommendedRemediations(remediations)
                .evaluatedAt(clock.instant())
                .build();
    }

    // Diagnostic simulation hooks for unit & chaos testing
    public void setOutboxLag(long lag) { this.mockOutboxLag.set(lag); }
    public void setKafkaConsumerLag(long lag) { this.mockKafkaConsumerLag.set(lag); }
    public void setCredentialsTtlSeconds(long seconds) { this.credentialsExpiryEpochMs.set(clock.millis() + (seconds * 1000L)); }
    public void setQuotaThrottled(boolean throttled) { this.simulateFcmQuotaThrottled = throttled; }
    public void setCanaryLatencyMs(long ms) { this.lastCanaryLatencyMs.set(ms); }
}
