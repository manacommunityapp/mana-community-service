package com.manacommunity.api.notification.service;

import com.manacommunity.api.exception.SmsRateLimitException;
import com.manacommunity.api.notification.config.SmsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window rate limiter (no Redis dependency).
 * Buckets: per-phone, per-user, per-IP — each keyed independently.
 */
@Component
@RequiredArgsConstructor
public class SmsRateLimiter {

    private final SmsProperties smsProperties;

    /** key → timestamps of recent requests within the window */
    private final ConcurrentHashMap<String, Deque<Instant>> windows = new ConcurrentHashMap<>();

    private static final long WINDOW_MILLIS = 60_000L; // 1 minute

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

    /** Periodic cleanup of stale keys to avoid unbounded map growth. */
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
