package com.manacommunity.api.notification.service;

import com.manacommunity.api.exception.SmsRateLimitException;
import com.manacommunity.api.notification.config.SmsProperties;
import com.manacommunity.platform.ratelimit.RedisSlidingWindowRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Distributed sliding-window SMS rate limiter backed by Redis + Lua when available,
 * with automatic fallback to an in-memory sliding window when Redis is unavailable.
 * Buckets: per-phone, per-user, per-IP — each keyed independently.
 */
@Component
public class SmsRateLimiter {

    private static final String REDIS_KEY_PREFIX = "sms:rl:";
    private static final long WINDOW_MILLIS = 60_000L; // 1 minute

    private final SmsProperties smsProperties;
    private final RedisSlidingWindowRateLimiter redisSlidingWindow;

    /** Fallback in-memory buckets: key → timestamps of recent requests within the window */
    private final ConcurrentHashMap<String, Deque<Instant>> windows = new ConcurrentHashMap<>();

    /** Convenience constructor for unit tests without Redis. */
    public SmsRateLimiter(SmsProperties smsProperties) {
        this(smsProperties, null);
    }

    @Autowired
    public SmsRateLimiter(
            SmsProperties smsProperties,
            @Autowired(required = false) RedisSlidingWindowRateLimiter redisSlidingWindow) {
        this.smsProperties = smsProperties;
        this.redisSlidingWindow = redisSlidingWindow;
    }

    /** Check & record a phone-based attempt. Throws SmsRateLimitException if over limit. */
    public void checkPhone(String normalizedPhone) {
        SmsProperties.RateLimit rl = smsProperties.getRateLimit();
        if (!rl.isEnabled()) return;
        check("phone:" + normalizedPhone, rl.getPerPhonePerMinute());
    }

    /** Check & record a user-based attempt. */
    public void checkUser(Long userId) {
        SmsProperties.RateLimit rl = smsProperties.getRateLimit();
        if (!rl.isEnabled() || userId == null) return;
        check("user:" + userId, rl.getPerUserPerMinute());
    }

    /** Check & record an IP-based attempt. */
    public void checkIp(String ip) {
        SmsProperties.RateLimit rl = smsProperties.getRateLimit();
        if (!rl.isEnabled() || ip == null) return;
        check("ip:" + ip, rl.getPerIpPerMinute());
    }

    private void check(String key, int limit) {
        if (redisSlidingWindow != null) {
            Boolean allowed = redisSlidingWindow.tryAcquire(REDIS_KEY_PREFIX + key, WINDOW_MILLIS, limit);
            if (allowed != null) {
                if (!allowed) {
                    throw new SmsRateLimitException(key);
                }
                return;
            }
        }
        checkInMemory(key, limit);
    }

    private void checkInMemory(String key, int limit) {
        Instant now = Instant.now();
        Instant cutoff = now.minusMillis(WINDOW_MILLIS);

        Deque<Instant> deque = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            // prune expired entries
            while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
                deque.pollFirst();
            }
            if (deque.size() >= limit) {
                throw new SmsRateLimitException(key);
            }
            deque.addLast(now);
        }
    }

    /** Periodic cleanup of stale in-memory fallback keys to avoid unbounded map growth. */
    public void evictStale() {
        Instant cutoff = Instant.now().minusMillis(WINDOW_MILLIS);
        windows.entrySet().removeIf(entry -> {
            Deque<Instant> deque = entry.getValue();
            synchronized (deque) {
                while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
                    deque.pollFirst();
                }
                return deque.isEmpty();
            }
        });
    }
}
