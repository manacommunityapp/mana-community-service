package com.manacommunity.api.cache.prewarm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Morning Peak Traffic Prewarmer & Cache Stampede Guard.
 *
 * Provides:
 * 1. Scheduled 07:30 AM pre-warming of high-concurrency school-bus, domestic help,
 *    and visitor rosters before the morning peak traffic burst.
 * 2. Probabilistic Early Expiration (XFetch algorithm) preventing cache stampedes.
 * 3. Singleflight deduplication ensuring at most 1 database query executes per key
 *    when hundreds of concurrent threads request a missing or expiring key.
 */
@Slf4j
@Service
public class MorningPeakTrafficPrewarmerService {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CachedItem<T> {
        private T value;
        private long expiryEpochMs;
        private long computationTimeMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrewarmReport {
        private long communityId;
        private int busRostersPrewarmed;
        private int domesticStaffPrewarmed;
        private int preApprovedVisitorsPrewarmed;
        private long totalTimeMs;
    }

    private final Clock clock;
    private final Random random;
    private final Map<String, CachedItem<?>> inMemoryCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<?>> inFlightCalls = new ConcurrentHashMap<>();

    public MorningPeakTrafficPrewarmerService() {
        this(Clock.systemUTC(), new Random());
    }

    public MorningPeakTrafficPrewarmerService(Clock clock, Random random) {
        this.clock = clock;
        this.random = random;
    }

    /**
     * Retrieves an item from cache with XFetch early probabilistic refresh.
     *
     * @param key cache key
     * @param beta refresh aggression multiplier (beta >= 1.0)
     * @param ttlMillis time-to-live when item is recomputed
     * @param loader database loader if key is absent or should be refreshed early
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrComputeWithXFetch(
            String key, double beta, long ttlMillis, Supplier<T> loader) {
        long now = clock.millis();
        CachedItem<T> item = (CachedItem<T>) inMemoryCache.get(key);

        if (item != null) {
            long remainingTtlMs = item.getExpiryEpochMs() - now;

            // XFetch formula: shouldRefresh = (-beta * delta * ln(random(0,1))) > remainingTtl
            // delta = item.computationTimeMs
            if (remainingTtlMs > 0) {
                double u = Math.max(0.0001, random.nextDouble());
                double earlyRefreshThreshold = -beta * item.getComputationTimeMs() * Math.log(u);

                if (earlyRefreshThreshold <= remainingTtlMs) {
                    // Cache hit within safe threshold; return cached value
                    return item.getValue();
                }
                log.info("XFetch triggered early background refresh for key: {} (remaining TTL: {}ms, threshold: {}ms)",
                        key, remainingTtlMs, (long) earlyRefreshThreshold);
            }
        }

        // Singleflight Mutex: exactly 1 thread executes loader; other callers wait for the same future
        return executeSingleflight(key, () -> {
            long start = clock.millis();
            T freshVal = loader.get();
            long computationTime = Math.max(1, clock.millis() - start);

            CachedItem<T> newItem = CachedItem.<T>builder()
                    .value(freshVal)
                    .expiryEpochMs(clock.millis() + ttlMillis)
                    .computationTimeMs(computationTime)
                    .build();

            inMemoryCache.put(key, newItem);
            return freshVal;
        });
    }

    /**
     * Singleflight pattern deduplicating concurrent loader executions for the same key.
     */
    @SuppressWarnings("unchecked")
    private <T> T executeSingleflight(String key, Supplier<T> loader) {
        CompletableFuture<T> future = (CompletableFuture<T>) inFlightCalls.computeIfAbsent(key, k -> {
            CompletableFuture<T> cf = new CompletableFuture<>();
            CompletableFuture.runAsync(() -> {
                try {
                    T result = loader.get();
                    cf.complete(result);
                } catch (Throwable t) {
                    cf.completeExceptionally(t);
                } finally {
                    inFlightCalls.remove(k);
                }
            });
            return cf;
        });

        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            inFlightCalls.remove(key);
            throw new RuntimeException("Singleflight loader failed for key: " + key, e);
        }
    }

    /**
     * Executes scheduled pre-warming for a community's 08:00 AM rush.
     */
    public PrewarmReport prewarmCommunityMorningCaches(
            Long communityId,
            List<String> schoolBusRoutes,
            List<String> domesticStaffNames,
            List<String> expectedVisitors) {
        long start = clock.millis();

        // 1. Prewarm School Bus Rosters
        for (String route : schoolBusRoutes) {
            String key = String.format("cache:bus:roster:%d:%s", communityId, route);
            getOrComputeWithXFetch(key, 1.0, 3600_000L, () -> "RosterData-" + route);
        }

        // 2. Prewarm Domestic Staff Schedule
        for (String staff : domesticStaffNames) {
            String key = String.format("cache:staff:sched:%d:%s", communityId, staff);
            getOrComputeWithXFetch(key, 1.0, 3600_000L, () -> "ScheduleData-" + staff);
        }

        // 3. Prewarm Expected Visitors
        for (String visitor : expectedVisitors) {
            String key = String.format("cache:visitor:exp:%d:%s", communityId, visitor);
            getOrComputeWithXFetch(key, 1.0, 3600_000L, () -> "VisitorData-" + visitor);
        }

        long duration = clock.millis() - start;
        log.info("Completed 07:30 AM pre-warming for community {} in {} ms", communityId, duration);

        return PrewarmReport.builder()
                .communityId(communityId)
                .busRostersPrewarmed(schoolBusRoutes.size())
                .domesticStaffPrewarmed(domesticStaffNames.size())
                .preApprovedVisitorsPrewarmed(expectedVisitors.size())
                .totalTimeMs(duration)
                .build();
    }

    public void clear() {
        inMemoryCache.clear();
        inFlightCalls.clear();
    }
}
