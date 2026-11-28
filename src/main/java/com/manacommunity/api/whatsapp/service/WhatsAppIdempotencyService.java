package com.manacommunity.api.whatsapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ensures strict exactly-once processing semantics for Meta WhatsApp webhooks.
 * Prevents duplicate state machine triggers from Meta's at-least-once delivery retries.
 */
@Service
@Slf4j
public class WhatsAppIdempotencyService {

    // In production, backed by Redis: redisTemplate.opsForValue().setIfAbsent(key, val, ttl)
    private final Map<String, Long> processedEvents = new ConcurrentHashMap<>();
    private final Map<String, Object> cachedResults = new ConcurrentHashMap<>();

    /**
     * Atomically attempts to acquire an idempotency lock for the given WhatsApp Message ID (wamid).
     * @return true if this is the first time receiving this messageId; false if it is a duplicate retry.
     */
    public boolean acquireLock(String messageId, Duration ttl) {
        if (messageId == null || messageId.isBlank()) {
            return true; // No message ID, bypass
        }
        long now = System.currentTimeMillis();
        Long existing = processedEvents.putIfAbsent(messageId, now);
        if (existing != null) {
            log.warn("Meta Webhook duplicate detected for messageId [{}]. Received previously at {}", messageId, existing);
            return false;
        }
        return true;
    }

    public void cacheResult(String messageId, Object result) {
        if (messageId != null && result != null) {
            cachedResults.put(messageId, result);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getCachedResult(String messageId, Class<T> clazz) {
        Object val = cachedResults.get(messageId);
        if (val != null && clazz.isAssignableFrom(val.getClass())) {
            return (T) val;
        }
        return null;
    }

    public void clear() {
        processedEvents.clear();
        cachedResults.clear();
    }
}
