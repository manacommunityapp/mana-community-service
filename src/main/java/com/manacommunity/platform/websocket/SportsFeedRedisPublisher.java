package com.manacommunity.platform.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import static com.manacommunity.platform.websocket.SportsWebSocketRedisConfig.FEED_EVENTS_CHANNEL;

/**
 * Publishes live-feed refresh events to the Redis Pub/Sub channel
 * {@value SportsWebSocketRedisConfig#FEED_EVENTS_CHANNEL}.
 *
 * <p>Falls back to a direct local STOMP broadcast when Redis is unavailable.</p>
 */
@Slf4j
@Component
public class SportsFeedRedisPublisher {

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate localMessagingTemplate;
    private final ObjectMapper objectMapper;

    public SportsFeedRedisPublisher(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Autowired(required = false) SimpMessagingTemplate localMessagingTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.localMessagingTemplate = localMessagingTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes a feed refresh event. Subscribers will broadcast
     * {@code {"type":"REFRESH"}} to the STOMP topic {@code /topic/sports/live}.
     *
     * @param payload the event payload (typically {@code Map.of("type","REFRESH")})
     */
    public void publishFeedRefresh(Object payload) {
        try {
            if (redisTemplate != null) {
                String json = objectMapper.writeValueAsString(payload);
                redisTemplate.convertAndSend(FEED_EVENTS_CHANNEL, json);
                log.debug("Published feed-refresh event to Redis channel {}", FEED_EVENTS_CHANNEL);
                return;
            }
        } catch (Exception e) {
            log.warn("Redis publish failed for feed refresh, falling back to local STOMP: {}", e.getMessage());
        }

        // Fallback: local in-memory broadcast
        if (localMessagingTemplate != null) {
            localMessagingTemplate.convertAndSend("/topic/sports/live", payload);
        }
    }
}
