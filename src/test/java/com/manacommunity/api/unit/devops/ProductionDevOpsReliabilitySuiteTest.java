package com.manacommunity.api.unit.devops;

import com.manacommunity.api.cache.prewarm.MorningPeakTrafficPrewarmerService;
import com.manacommunity.api.cache.prewarm.MorningPeakTrafficPrewarmerService.PrewarmReport;
import com.manacommunity.api.db.migration.ZeroDowntimeMigrationSafetyValidator;
import com.manacommunity.api.db.migration.ZeroDowntimeMigrationSafetyValidator.MigrationPlan;
import com.manacommunity.api.db.migration.ZeroDowntimeMigrationSafetyValidator.ValidationReport;
import com.manacommunity.api.notification.diagnostics.PushNotificationDiagnosticsService;
import com.manacommunity.api.notification.diagnostics.PushNotificationDiagnosticsService.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionDevOpsReliabilitySuiteTest {

    private MutableClock clock;
    private PushNotificationDiagnosticsService diagnosticsService;
    private MorningPeakTrafficPrewarmerService prewarmerService;
    private ZeroDowntimeMigrationSafetyValidator migrationValidator;

    static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneOffset zone = ZoneOffset.UTC;

        public MutableClock(Instant instant) {
            this.instant = instant;
        }

        public void advanceSeconds(long sec) {
            this.instant = this.instant.plusSeconds(sec);
        }

        @Override public ZoneOffset getZone() { return zone; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-27T08:00:00Z"));
        diagnosticsService = new PushNotificationDiagnosticsService(clock);
        prewarmerService = new MorningPeakTrafficPrewarmerService(clock, new Random(42));
        migrationValidator = new ZeroDowntimeMigrationSafetyValidator();
    }

    // ── Incident 1: 08:00 AM Morning Peak Pre-Warming & Cache Stampede Guard ──

    @Test
    @DisplayName("Prewarmer pre-populates school bus, domestic staff, and visitor rosters before 08:00 AM burst")
    void prewarmer_prewarmsCommunityCachesBeforeRushHour() {
        PrewarmReport report = prewarmerService.prewarmCommunityMorningCaches(
                101L,
                List.of("Route-42", "Route-10", "Route-07"),
                List.of("Sunita Cook", "Ramesh Driver", "Anita Maid"),
                List.of("Delivery-Blinkit-101", "Delivery-Amazon-402")
        );

        assertThat(report.getCommunityId()).isEqualTo(101L);
        assertThat(report.getBusRostersPrewarmed()).isEqualTo(3);
        assertThat(report.getDomesticStaffPrewarmed()).isEqualTo(3);
        assertThat(report.getPreApprovedVisitorsPrewarmed()).isEqualTo(2);

        // Subsequent fetch hits warm cache without invoking loader
        AtomicInteger loaderCalls = new AtomicInteger(0);
        String cachedBus = prewarmerService.getOrComputeWithXFetch(
                "cache:bus:roster:101:Route-42", 1.0, 3600_000L, () -> {
                    loaderCalls.incrementAndGet();
                    return "DatabaseValue";
                });

        assertThat(cachedBus).isEqualTo("RosterData-Route-42");
        assertThat(loaderCalls.get()).isEqualTo(0); // Never hit database!
    }

    @Test
    @DisplayName("Singleflight mutex ensures only 1 database query executes during concurrent cache misses")
    void singleflight_deduplicatesConcurrentDatabaseQueries() throws Exception {
        int concurrentThreads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(concurrentThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger databaseQueryCount = new AtomicInteger(0);

        List<Future<String>> futures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < concurrentThreads; i++) {
            futures.add(pool.submit(() -> {
                latch.await();
                return prewarmerService.getOrComputeWithXFetch(
                        "cache:hot:school-bus:live-route", 1.0, 60_000L, () -> {
                            databaseQueryCount.incrementAndGet();
                            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                            return "HeavyBusGeometryData";
                        });
            }));
        }

        latch.countDown(); // Release all 20 threads simultaneously

        for (Future<String> f : futures) {
            assertThat(f.get(2, TimeUnit.SECONDS)).isEqualTo("HeavyBusGeometryData");
        }

        // Exactly 1 database query executed; remaining 19 concurrent threads shared the single in-flight result!
        assertThat(databaseQueryCount.get()).isEqualTo(1);
        pool.shutdown();
    }

    // ── Incident 2: Zero-Downtime Migration Safety on 800M-Row Table ──────────

    @Test
    @DisplayName("Rejects Native ALTER and Trigger-based tools on 800M-row table receiving 3,000 writes/sec")
    void migrationValidator_blocksDangerousMigrationPlans() {
        MigrationPlan dangerousNativePlan = MigrationPlan.builder()
                .tableName("visitor_entries")
                .estimatedRowCount(800_000_000L)
                .peakWritesPerSecond(3_000)
                .tool("NATIVE_ALTER")
                .chunkSize(5000)
                .maxReplicaLagMillis(5000)
                .lockWaitTimeoutSeconds(60)
                .useTriggers(false)
                .cutoverMethod("LOCK_AND_SWAP")
                .build();

        ValidationReport report1 = migrationValidator.evaluateMigrationPlan(dangerousNativePlan);
        assertThat(report1.isApproved()).isFalse();
        assertThat(report1.getCriticalViolations()).anyMatch(v -> v.contains("Native ALTER TABLE prohibited"));
        assertThat(report1.getCriticalViolations()).anyMatch(v -> v.contains("Chunk size"));
        assertThat(report1.getCriticalViolations()).anyMatch(v -> v.contains("Lock wait timeout"));

        MigrationPlan triggerPlan = MigrationPlan.builder()
                .tableName("visitor_entries")
                .estimatedRowCount(800_000_000L)
                .peakWritesPerSecond(3_000)
                .tool("PT_ONLINE_SCHEMA_CHANGE")
                .chunkSize(1000)
                .maxReplicaLagMillis(1000)
                .lockWaitTimeoutSeconds(2)
                .useTriggers(true) // Triggers at 3k writes/sec!
                .cutoverMethod("ATOMIC_RENAME")
                .build();

        ValidationReport report2 = migrationValidator.evaluateMigrationPlan(triggerPlan);
        assertThat(report2.isApproved()).isFalse();
        assertThat(report2.getCriticalViolations()).anyMatch(v -> v.contains("Trigger-based migrations"));
    }

    @Test
    @DisplayName("Approves non-blocking gh-ost migration plan with binlog streaming, throttled chunks, and atomic rename")
    void migrationValidator_approvesCompliantGhostPlan() {
        MigrationPlan ghostPlan = MigrationPlan.builder()
                .tableName("visitor_entries")
                .estimatedRowCount(800_000_000L)
                .peakWritesPerSecond(3_000)
                .tool("GH_OST")
                .chunkSize(1500)
                .maxReplicaLagMillis(1000)
                .maxThreadsRunning(45)
                .lockWaitTimeoutSeconds(2)
                .useTriggers(false)
                .cutoverMethod("ATOMIC_RENAME")
                .defaultColumnClause("DEFAULT 'LEGACY_DEFAULT'")
                .build();

        ValidationReport report = migrationValidator.evaluateMigrationPlan(ghostPlan);
        assertThat(report.isApproved()).isTrue();
        assertThat(report.getExecutionMode()).isEqualTo("PRODUCTION_READY_NON_BLOCKING");
        assertThat(report.getCriticalViolations()).isEmpty();
    }

    // ── Incident 3: Push Notification Blackhole & Canary Heartbeat Diagnostics ─

    @Test
    @DisplayName("Detects Kafka consumer lag, expired credentials, and outbox backlogs in diagnostic evaluation")
    void diagnostics_identifiesBlackholeBottlenecks() {
        // Initially healthy
        BlackholeDiagnosticReport healthy = diagnosticsService.evaluatePipelineHealth();
        assertThat(healthy.getStatus()).isEqualTo(PipelineHealthStatus.HEALTHY);
        assertThat(healthy.getDetectedBottlenecks()).isEmpty();

        // 1. Simulate Outbox Relay failure (500 events pending)
        diagnosticsService.setOutboxLag(500);
        BlackholeDiagnosticReport degraded = diagnosticsService.evaluatePipelineHealth();
        assertThat(degraded.getStatus()).isEqualTo(PipelineHealthStatus.DEGRADED);
        assertThat(degraded.getDetectedBottlenecks()).anyMatch(b -> b.contains("Outbox relay backlog"));

        // 2. Simulate Kafka Consumer crash / poison pill (1500 events lag)
        diagnosticsService.setKafkaConsumerLag(1500);
        BlackholeDiagnosticReport blackhole = diagnosticsService.evaluatePipelineHealth();
        assertThat(blackhole.getStatus()).isEqualTo(PipelineHealthStatus.CRITICAL_BLACKHOLE);
        assertThat(blackhole.getDetectedBottlenecks()).anyMatch(b -> b.contains("Critical Kafka consumer lag"));

        // 3. Simulate FCM OAuth token expiry
        diagnosticsService.setCredentialsTtlSeconds(-10); // Expired 10 seconds ago
        BlackholeDiagnosticReport credsExpired = diagnosticsService.evaluatePipelineHealth();
        assertThat(credsExpired.isCredentialsValid()).isFalse();
        assertThat(credsExpired.getDetectedBottlenecks()).anyMatch(b -> b.contains("EXPIRED"));
        assertThat(credsExpired.getRecommendedRemediations()).anyMatch(r -> r.contains("Secret Manager"));
    }

    @Test
    @DisplayName("Enforces high-priority payload for time-critical gate visitor passes to prevent Android Doze delay")
    void diagnostics_enforcesHighPriorityForDozeCompliance() {
        // Gate arrival alert with NORMAL priority -> FAILS compliance (would be delayed by Doze)
        PushDeliveryRequest lazyGateAlert = PushDeliveryRequest.builder()
                .eventType("VISITOR_ARRIVED")
                .priority("NORMAL")
                .build();
        assertThat(diagnosticsService.validatePayloadPriorityForDozeCompliance(lazyGateAlert)).isFalse();

        // Gate arrival alert with HIGH priority -> PASSES compliance
        PushDeliveryRequest urgentGateAlert = PushDeliveryRequest.builder()
                .eventType("VISITOR_ARRIVED")
                .priority("HIGH")
                .build();
        assertThat(diagnosticsService.validatePayloadPriorityForDozeCompliance(urgentGateAlert)).isTrue();
    }

    @Test
    @DisplayName("Synthetic canary probe measures end-to-end delivery latency and detects SLA breach")
    void diagnostics_canaryMeasuresLatencyAndBreach() {
        String probeId = diagnosticsService.dispatchCanaryProbe();
        assertThat(probeId).startsWith("canary-");

        // Physical device confirms receipt 250ms later
        clock.advanceSeconds(0);
        long latency = diagnosticsService.recordCanaryReceipt(probeId);
        assertThat(latency).isGreaterThanOrEqualTo(0);

        // If canary latency explodes to 20 seconds (>15s SLA), diagnostic report flags warning
        diagnosticsService.setCanaryLatencyMs(20_000L);
        BlackholeDiagnosticReport report = diagnosticsService.evaluatePipelineHealth();
        assertThat(report.getDetectedBottlenecks()).anyMatch(b -> b.contains("Canary heartbeat latency"));
        assertThat(report.getRecommendedRemediations()).anyMatch(r -> r.contains("Page on-call SRE"));
    }
}
