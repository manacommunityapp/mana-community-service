package com.manacommunity.platform.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks active WebSocket sessions per user.
 * Uses Redis for multi-instance coordination; falls back to in-memory when Redis is unavailable.
 */
@Slf4j
@Component
public class WebSocketSessionTracker {

    private static final String KEY_PREFIX = "ws:sessions:";
    private static final Duration SESSION_TTL = Duration.ofHours(24);
    private static final int MAX_CONNECTIONS_PER_USER = 5;

    private final StringRedisTemplate redisTemplate;

    // In-memory fallback: userId → active session count
    private final ConcurrentHashMap<Long, AtomicInteger> localCounts = new ConcurrentHashMap<>();
    // Session → userId mapping (needed for disconnect cleanup in fallback mode)
    private final ConcurrentHashMap<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    public WebSocketSessionTracker(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void registerSession(Long userId, String sessionId) {
        sessionUserMap.put(sessionId, userId);
        if (redisTemplate == null) {
            localCounts.computeIfAbsent(userId, id -> new AtomicInteger(0)).incrementAndGet();
            return;
        }
        try {
            String key = KEY_PREFIX + userId;
            redisTemplate.opsForSet().add(key, sessionId);
            redisTemplate.expire(key, SESSION_TTL);
        } catch (Exception e) {
            log.debug("Redis unavailable for session tracking, using local counter: {}", e.getMessage());
            localCounts.computeIfAbsent(userId, id -> new AtomicInteger(0)).incrementAndGet();
        }
    }

    public void unregisterSession(Long userId, String sessionId) {
        sessionUserMap.remove(sessionId);
        if (redisTemplate == null) {
            AtomicInteger counter = localCounts.get(userId);
            if (counter != null) {
                int remaining = counter.decrementAndGet();
                if (remaining <= 0) localCounts.remove(userId);
            }
            return;
        }
        try {
            redisTemplate.opsForSet().remove(KEY_PREFIX + userId, sessionId);
        } catch (Exception e) {
            log.debug("Redis unavailable for session unregistration: {}", e.getMessage());
            AtomicInteger counter = localCounts.get(userId);
            if (counter != null) {
                int remaining = counter.decrementAndGet();
                if (remaining <= 0) localCounts.remove(userId);
            }
        }
    }

    public int getConnectionCount(Long userId) {
        if (redisTemplate == null) {
            AtomicInteger counter = localCounts.get(userId);
            return counter != null ? counter.get() : 0;
        }
        try {
            Long count = redisTemplate.opsForSet().size(KEY_PREFIX + userId);
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.debug("Redis unavailable, falling back to local count: {}", e.getMessage());
            AtomicInteger counter = localCounts.get(userId);
            return counter != null ? counter.get() : 0;
        }
    }

    public boolean isConnectionLimitReached(Long userId) {
        return getConnectionCount(userId) >= MAX_CONNECTIONS_PER_USER;
    }

    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        Long userId = sessionUserMap.get(sessionId);
        if (userId != null) {
            unregisterSession(userId, sessionId);
            log.debug("WebSocket session disconnected: user={} session={}", userId, sessionId);
        }
    }
}
