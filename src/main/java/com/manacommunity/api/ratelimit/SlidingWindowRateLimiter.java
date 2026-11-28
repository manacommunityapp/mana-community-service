package com.manacommunity.api.ratelimit;

import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class SlidingWindowRateLimiter {

    private final ConcurrentHashMap<String, WindowState> windows = new ConcurrentHashMap<>();
    private final int defaultLimit;
    private final long windowMillis;
    private final ConcurrentHashMap<String, Integer> burstOverrides = new ConcurrentHashMap<>();

    public SlidingWindowRateLimiter(int defaultLimit, long windowMillis) {
        this.defaultLimit = defaultLimit;
        this.windowMillis = windowMillis;
    }

    public void setBurstLimit(String key, int limit) {
        burstOverrides.put(key, limit);
    }

    public RateLimitResult tryAcquire(String key) {
        WindowState state = windows.computeIfAbsent(key, k -> new WindowState());
        long now = Instant.now().toEpochMilli();

        state.evictExpired(now, windowMillis);

        int limit = burstOverrides.getOrDefault(key, defaultLimit);
        int currentCount = state.count.get();

        if (currentCount >= limit) {
            long oldestTimestamp = state.timestamps.isEmpty() ? now : state.timestamps.peekFirst();
            long retryAfterMs = (oldestTimestamp + windowMillis) - now;
            return new RateLimitResult(false, currentCount, limit, Math.max(0, retryAfterMs));
        }

        state.timestamps.addLast(now);
        int newCount = state.count.incrementAndGet();
        return new RateLimitResult(true, newCount, limit, 0);
    }

    public RateLimitInfo getInfo(String key) {
        WindowState state = windows.get(key);
        if (state == null) {
            int limit = burstOverrides.getOrDefault(key, defaultLimit);
            return new RateLimitInfo(key, 0, limit, limit);
        }
        state.evictExpired(Instant.now().toEpochMilli(), windowMillis);
        int limit = burstOverrides.getOrDefault(key, defaultLimit);
        int used = state.count.get();
        return new RateLimitInfo(key, used, limit, Math.max(0, limit - used));
    }

    public void reset(String key) {
        windows.remove(key);
    }

    public int activeKeys() {
        return windows.size();
    }

    private static class WindowState {
        final Deque<Long> timestamps = new ConcurrentLinkedDeque<>();
        final AtomicInteger count = new AtomicInteger(0);

        void evictExpired(long now, long windowMillis) {
            long cutoff = now - windowMillis;
            while (!timestamps.isEmpty()) {
                Long oldest = timestamps.peekFirst();
                if (oldest != null && oldest < cutoff) {
                    timestamps.pollFirst();
                    count.decrementAndGet();
                } else {
                    break;
                }
            }
        }
    }

    public record RateLimitResult(boolean allowed, int currentCount, int limit, long retryAfterMs) {}
    public record RateLimitInfo(String key, int used, int limit, int remaining) {}
}
