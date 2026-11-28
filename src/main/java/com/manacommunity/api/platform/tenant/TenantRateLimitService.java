package com.manacommunity.api.platform.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-performance per-community rate limiter keyed by communityId.
 *
 * <p>Enforces API traffic quotas before requests reach service execution threads or the database.
 * Supports distributed Redis token/sliding-window counting with seamless in-memory fallback.</p>
 */
@Service
@Slf4j
public class TenantRateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final int defaultRequestsPerMinute;
    private final ConcurrentHashMap<Long, Integer> customTenantLimits = new ConcurrentHashMap<>();

    // In-memory sliding window fallback: "communityId:minuteBucket" -> counter
    private final ConcurrentHashMap<String, AtomicInteger> inMemoryBuckets = new ConcurrentHashMap<>();

    public TenantRateLimitService(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Value("${mana.tenant.rate-limit.default-per-minute:600}") int defaultRequestsPerMinute
    ) {
        this.redisTemplate = redisTemplate;
        this.defaultRequestsPerMinute = defaultRequestsPerMinute;
        log.info("[TenantRateLimit] Initialized with default limit of {} req/min per tenant", defaultRequestsPerMinute);
    }

    public void setTenantLimit(Long communityId, int requestsPerMinute) {
        customTenantLimits.put(communityId, requestsPerMinute);
        log.info("[TenantRateLimit] Custom limit configured for community {}: {} req/min", communityId, requestsPerMinute);
    }

    public TenantRateLimitResult checkTenant(Long communityId) {
        if (communityId == null || communityId <= 0L) {
            // Unauthenticated or platform calls pass through
            return new TenantRateLimitResult(true, 10000, 10000, Instant.now().getEpochSecond() + 60);
        }

        int maxAllowed = customTenantLimits.getOrDefault(communityId, defaultRequestsPerMinute);
        long currentEpochMinute = Instant.now().getEpochSecond() / 60;
        long resetEpochSecond = (currentEpochMinute + 1) * 60;

        if (redisTemplate != null) {
            try {
                String redisKey = "ratelimit:tenant:" + communityId + ":" + currentEpochMinute;
                Long count = redisTemplate.opsForValue().increment(redisKey);
                if (count != null && count == 1) {
                    redisTemplate.expire(redisKey, 90, TimeUnit.SECONDS);
                }

                int currentCount = count != null ? count.intValue() : 1;
                int remaining = Math.max(0, maxAllowed - currentCount);
                boolean allowed = currentCount <= maxAllowed;

                if (!allowed) {
                    log.warn("[TenantRateLimit] Community {} exceeded rate quota: count={}/{} in current minute",
                            communityId, currentCount, maxAllowed);
                }

                return new TenantRateLimitResult(allowed, maxAllowed, remaining, resetEpochSecond);
            } catch (Exception e) {
                log.warn("[TenantRateLimit] Redis error, falling back to in-memory limiter: {}", e.getMessage());
            }
        }

        // In-memory fallback
        String bucketKey = communityId + ":" + currentEpochMinute;
        AtomicInteger counter = inMemoryBuckets.computeIfAbsent(bucketKey, k -> new AtomicInteger(0));
        int currentCount = counter.incrementAndGet();

        // Prune old in-memory buckets periodically
        if (inMemoryBuckets.size() > 5000) {
            long prevMinute = currentEpochMinute - 2;
            inMemoryBuckets.keySet().removeIf(k -> {
                String[] parts = k.split(":");
                return parts.length == 2 && Long.parseLong(parts[1]) < prevMinute;
            });
        }

        int remaining = Math.max(0, maxAllowed - currentCount);
        boolean allowed = currentCount <= maxAllowed;

        if (!allowed) {
            log.warn("[TenantRateLimit] Community {} exceeded in-memory rate quota: count={}/{} in current minute",
                    communityId, currentCount, maxAllowed);
        }

        return new TenantRateLimitResult(allowed, maxAllowed, remaining, resetEpochSecond);
    }

    public void reset() {
        customTenantLimits.clear();
        inMemoryBuckets.clear();
    }

    public record TenantRateLimitResult(
            boolean allowed,
            int limit,
            int remaining,
            long resetEpochSecond
    ) {}
}
