package com.manacommunity.platform.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Periodically cleans up stale WebSocket session entries in Redis.
 * This catches any sessions that disconnected without firing a {@link org.springframework.web.socket.messaging.SessionDisconnectEvent}.
 */
@Slf4j
@Component
public class StaleSessionReaper {

    private static final String SESSION_KEY_PATTERN = "ws:sessions:*";
    private static final int MAX_SCAN_COUNT = 200;

    private final StringRedisTemplate redisTemplate;
    private final WebSocketSessionTracker sessionTracker;

    public StaleSessionReaper(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Autowired(required = false) WebSocketSessionTracker sessionTracker) {
        this.redisTemplate = redisTemplate;
        this.sessionTracker = sessionTracker;
    }

    /**
     * Every 5 minutes, remove orphaned WebSocket sessions left by dead nodes and
     * any empty session sets in Redis.
     */
    @Scheduled(fixedDelayString = "${app.websocket.session-reaper.interval-ms:300000}")
    public void reapStaleSessions() {
        if (redisTemplate == null) return;
        try {
            int reaped = 0;
            if (sessionTracker != null) {
                reaped += sessionTracker.reapStaleSessions();
            } else {
                Set<String> keys = redisTemplate.keys(SESSION_KEY_PATTERN);
                if (keys != null) {
                    for (String key : keys) {
                        Long size = redisTemplate.opsForSet().size(key);
                        if (size != null && size == 0) {
                            redisTemplate.delete(key);
                            reaped++;
                        }
                    }
                }
            }
            if (reaped > 0) {
                log.info("StaleSessionReaper: removed {} stale WebSocket session entries/keys", reaped);
            }
        } catch (Exception e) {
            log.debug("StaleSessionReaper: Redis unavailable, skipping reap: {}", e.getMessage());
        }
    }
}
