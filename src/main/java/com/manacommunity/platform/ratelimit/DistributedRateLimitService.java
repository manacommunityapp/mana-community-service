package com.manacommunity.platform.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Distributed rate limiter using Redis INCR+EXPIRE. Falls back to an in-memory
 * ConcurrentHashMap when Redis is unavailable — safety is preserved; limits still apply.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedRateLimitService {

    private final StringRedisTemplate redis;
    private final RateLimitConfig config;

    // Fallback in-memory buckets: key → {count, windowStart}
    private final ConcurrentHashMap<String, long[]> memoryBuckets = new ConcurrentHashMap<>();

    public RateLimitResult checkIpLimit(String ip, RateLimitType type) {
        return check("ip:" + sanitize(ip), type);
    }

    public RateLimitResult checkAccountLimit(String accountKey, RateLimitType type) {
        return check("acct:" + sanitize(accountKey), type);
    }

    /** Both IP and account must pass. Returns the more restrictive result. */
    public RateLimitResult check(String ip, String accountKey, RateLimitType type) {
        RateLimitResult ipResult  = checkIpLimit(ip, type);
        if (!ipResult.allowed()) return ipResult;
        RateLimitResult acctResult = checkAccountLimit(accountKey, type);
        return acctResult.allowed() ? ipResult : acctResult;
    }

    private RateLimitResult check(String subKey, RateLimitType type) {
        RateLimitConfig.EndpointLimit limit = config.forType(type);
        String key = "rl:" + type.name().toLowerCase() + ":" + subKey;
        try {
            Long count = redis.opsForValue().increment(key);
            if (count == null) count = 1L;
            if (count == 1L) {
                redis.expire(key, limit.windowSeconds(), TimeUnit.SECONDS);
            }
            Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
            long resetAt = Instant.now().getEpochSecond() + (ttl != null && ttl > 0 ? ttl : limit.windowSeconds());
            int remaining = Math.max(0, limit.maxRequests() - count.intValue());
            return new RateLimitResult(count <= limit.maxRequests(), remaining, resetAt);
        } catch (Exception e) {
            log.warn("Redis unavailable for rate limit key={}, falling back to in-memory: {}", key, e.getMessage());
            return checkInMemory(key, limit);
        }
    }

    private RateLimitResult checkInMemory(String key, RateLimitConfig.EndpointLimit limit) {
        long now = Instant.now().getEpochSecond();
        long[] bucket = memoryBuckets.compute(key, (k, existing) -> {
            if (existing == null || now - existing[1] >= limit.windowSeconds()) {
                return new long[]{1, now};
            }
            existing[0]++;
            return existing;
        });
        long resetAt = bucket[1] + limit.windowSeconds();
        int remaining = Math.max(0, limit.maxRequests() - (int) bucket[0]);
        return new RateLimitResult(bucket[0] <= limit.maxRequests(), remaining, resetAt);
    }

    private static String sanitize(String value) {
        if (value == null) return "unknown";
        return value.replaceAll("[^a-zA-Z0-9.:_-]", "_");
    }

    public record RateLimitResult(boolean allowed, int remaining, long resetAtEpochSecond) {}
}
