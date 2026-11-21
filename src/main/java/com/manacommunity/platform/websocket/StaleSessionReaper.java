package com.manacommunity.platform.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class StaleSessionReaper {

    private static final String SESSION_KEY_PATTERN = "ws:sessions:*";
    private static final int MAX_SCAN_COUNT = 200;

    private final StringRedisTemplate redisTemplate;

    /**
     * Every 5 minutes, scan for user session sets that have zero members and
     * remove the empty key to avoid unbounded growth of stale Redis keys.
     */
    @Scheduled(fixedDelayString = "${app.websocket.session-reaper.interval-ms:300000}")
    public void reapStaleSessions() {
        try {
            Set<String> keys = redisTemplate.keys(SESSION_KEY_PATTERN);
            if (keys == null || keys.isEmpty()) return;

            int reaped = 0;
            for (String key : keys) {
                Long size = redisTemplate.opsForSet().size(key);
                if (size != null && size == 0) {
                    redisTemplate.delete(key);
                    reaped++;
                }
            }
            if (reaped > 0) {
                log.info("StaleSessionReaper: removed {} empty session keys", reaped);
            }
        } catch (Exception e) {
            log.debug("StaleSessionReaper: Redis unavailable, skipping reap: {}", e.getMessage());
        }
    }
}
