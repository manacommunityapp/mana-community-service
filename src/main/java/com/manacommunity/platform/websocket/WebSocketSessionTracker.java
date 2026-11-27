package com.manacommunity.platform.websocket;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks active WebSocket sessions per user.
 * Uses Redis for multi-instance coordination with per-node heartbeats so crashed or
 * restarted instances never leave permanent stale keys; falls back to in-memory when Redis is unavailable.
 */
@Slf4j
@Component
public class WebSocketSessionTracker {

    private static final String KEY_PREFIX = "ws:sessions:";
    private static final String SESSION_NODE_PREFIX = "ws:session-node:";
    private static final String NODE_ALIVE_PREFIX = "ws:node:";
    private static final Duration SESSION_TTL = Duration.ofHours(24);
    private static final Duration NODE_HEARTBEAT_TTL = Duration.ofSeconds(90);
    private static final int MAX_CONNECTIONS_PER_USER = 5;

    private final String instanceId = UUID.randomUUID().toString();
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
            redisTemplate.opsForValue().set(SESSION_NODE_PREFIX + sessionId, instanceId, SESSION_TTL);
            redisTemplate.opsForValue().set(NODE_ALIVE_PREFIX + instanceId, "1", NODE_HEARTBEAT_TTL);
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
            String key = KEY_PREFIX + userId;
            redisTemplate.opsForSet().remove(key, sessionId);
            redisTemplate.delete(SESSION_NODE_PREFIX + sessionId);
            Long size = redisTemplate.opsForSet().size(key);
            if (size != null && size == 0) {
                redisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.debug("Redis unavailable for session unregistration: {}", e.getMessage());
            AtomicInteger counter = localCounts.get(userId);
            if (counter != null) {
                int remaining = counter.decrementAndGet();
                if (remaining <= 0) localCounts.remove(userId);
            }
        }
    }

    @Scheduled(fixedRate = 30_000)
    public void refreshNodeHeartbeat() {
        if (redisTemplate == null || sessionUserMap.isEmpty()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(NODE_ALIVE_PREFIX + instanceId, "1", NODE_HEARTBEAT_TTL);
        } catch (Exception e) {
            log.debug("Redis unavailable while refreshing WebSocket node heartbeat: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void cleanupLocalSessions() {
        if (sessionUserMap.isEmpty() && redisTemplate == null) {
            return;
        }
        for (Map.Entry<String, Long> entry : sessionUserMap.entrySet()) {
            unregisterSession(entry.getValue(), entry.getKey());
        }
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(NODE_ALIVE_PREFIX + instanceId);
            } catch (Exception ignored) {
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

    /**
     * Checks if there are any active WebSocket client connections, purging stale
     * session entries left behind by dead or restarted JVM instances.
     */
    public boolean hasActiveSessions() {
        if (!sessionUserMap.isEmpty()) {
            return true;
        }
        if (redisTemplate == null) {
            return false;
        }
        try {
            reapStaleSessions();
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
            return keys != null && !keys.isEmpty();
        } catch (Exception e) {
            log.debug("Redis unavailable while checking active sessions: {}", e.getMessage());
            return !sessionUserMap.isEmpty();
        }
    }

    /**
     * Removes empty user session sets and individual sessions whose owning node is no longer alive.
     *
     * @return number of stale keys or session entries removed
     */
    public int reapStaleSessions() {
        if (redisTemplate == null) return 0;
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) return 0;

        int reaped = 0;
        for (String key : keys) {
            Set<String> members = redisTemplate.opsForSet().members(key);
            if (members == null || members.isEmpty()) {
                redisTemplate.delete(key);
                reaped++;
                continue;
            }
            for (String sessionId : members) {
                if (sessionUserMap.containsKey(sessionId)) {
                    continue;
                }
                String ownerNode = redisTemplate.opsForValue().get(SESSION_NODE_PREFIX + sessionId);
                boolean nodeAlive = ownerNode != null
                        && Boolean.TRUE.equals(redisTemplate.hasKey(NODE_ALIVE_PREFIX + ownerNode));
                if (!nodeAlive) {
                    redisTemplate.opsForSet().remove(key, sessionId);
                    redisTemplate.delete(SESSION_NODE_PREFIX + sessionId);
                    reaped++;
                }
            }
            Long remaining = redisTemplate.opsForSet().size(key);
            if (remaining != null && remaining == 0) {
                redisTemplate.delete(key);
            }
        }
        return reaped;
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
