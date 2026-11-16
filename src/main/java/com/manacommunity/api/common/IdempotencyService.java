package com.manacommunity.api.common;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process idempotency guard for transaction endpoints.
 * Keys expire after TTL_SECONDS to prevent unbounded growth.
 */
@Service
public class IdempotencyService {

    private static final long TTL_SECONDS = 86_400; // 24 hours

    private final ConcurrentHashMap<String, Instant> seen = new ConcurrentHashMap<>();

    /**
     * Registers the key and returns normally on first call.
     * Throws IllegalStateException if the key was already seen within the TTL.
     */
    public void checkAndRegister(String key) {
        if (key == null || key.isBlank()) {
            return; // no key supplied — skip idempotency check
        }
        evictExpired();
        Instant existing = seen.putIfAbsent(key, Instant.now());
        if (existing != null) {
            throw new IllegalStateException("Duplicate request: idempotency key already used.");
        }
    }

    private void evictExpired() {
        Instant cutoff = Instant.now().minusSeconds(TTL_SECONDS);
        seen.entrySet().removeIf(e -> e.getValue().isBefore(cutoff));
    }
}
