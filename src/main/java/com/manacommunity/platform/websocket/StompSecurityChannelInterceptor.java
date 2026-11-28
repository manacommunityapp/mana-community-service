package com.manacommunity.platform.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.time.Duration;

/**
 * Additional STOMP security: connection limits, client→topic send block, message rate limits.
 * Registered after the existing auth interceptor and {@link com.manacommunity.api.security.StompSubscriptionInterceptor}.
 */
@Slf4j
@Component
public class StompSecurityChannelInterceptor implements ChannelInterceptor {

    private static final int MSG_RATE_LIMIT = 60;       // messages per window
    private static final Duration MSG_WINDOW = Duration.ofMinutes(1);
    private static final String MSG_RATE_KEY_PREFIX = "ws:msg:";

    private final WebSocketSessionTracker sessionTracker;
    private final StringRedisTemplate redisTemplate;

    public StompSecurityChannelInterceptor(WebSocketSessionTracker sessionTracker,
                                           @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.sessionTracker = sessionTracker;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        StompCommand command = accessor.getCommand();
        if (command == null) return message;

        Long userId = extractUserId(accessor);

        return switch (command) {
            case CONNECT -> handleConnect(message, userId, accessor.getSessionId());
            case SEND -> handleSend(message, userId, accessor.getDestination());
            default -> message;
        };
    }

    private Message<?> handleConnect(Message<?> message, Long userId, String sessionId) {
        if (userId == null) {
            // Authentication interceptor runs before us; unauthenticated CONNECTs have no userId
            // but are permitted to pass through — the auth interceptor already set user=null
            return message;
        }
        if (sessionTracker.isConnectionLimitReached(userId)) {
            log.warn("WebSocket connection limit reached: user={}", userId);
            return null; // reject CONNECT frame
        }
        sessionTracker.registerSession(userId, sessionId != null ? sessionId : "unknown");
        return message;
    }

    private Message<?> handleSend(Message<?> message, Long userId, String destination) {
        // Block clients from sending directly to broker topics (they must use /app/)
        if (destination != null && destination.startsWith("/topic/")) {
            log.warn("STOMP SEND to /topic/ blocked: user={} destination={}", userId, destination);
            return null;
        }

        // Message rate limit for authenticated users
        if (userId != null && !isWithinMessageRateLimit(userId)) {
            log.warn("WebSocket message rate limit exceeded: user={}", userId);
            return null;
        }

        return message;
    }

    private boolean isWithinMessageRateLimit(Long userId) {
        if (redisTemplate == null) return true;
        String key = MSG_RATE_KEY_PREFIX + userId;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == 1) {
                redisTemplate.expire(key, MSG_WINDOW);
            }
            return count != null && count <= MSG_RATE_LIMIT;
        } catch (Exception e) {
            log.debug("Redis unavailable for WS rate limit, allowing message: {}", e.getMessage());
            return true; // fail-open for message rate when Redis is down
        }
    }

    private Long extractUserId(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            Object p = auth.getPrincipal();
            if (p instanceof String s) {
                try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }
}
