package com.manacommunity.platform.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import static com.manacommunity.platform.websocket.SportsWebSocketRedisConfig.EVENT_AUCTION_CHANNEL;

/**
 * Publishes item-auction bid events to the Redis Pub/Sub channel
 * {@value SportsWebSocketRedisConfig#EVENT_AUCTION_CHANNEL}.
 *
 * <p>Falls back to a direct local STOMP broadcast when Redis is unavailable.</p>
 */
@Slf4j
@Component
public class EventAuctionRedisPublisher {

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate localMessagingTemplate;
    private final ObjectMapper objectMapper;

    public EventAuctionRedisPublisher(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Autowired(required = false) SimpMessagingTemplate localMessagingTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.localMessagingTemplate = localMessagingTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes a bid event for the given auction item.
     *
     * @param itemId  the auction item ID (routed to {@code /topic/auction/{itemId}})
     * @param payload the event payload map
     */
    public void publishBidEvent(Long itemId, Object payload) {
        try {
            if (redisTemplate != null) {
                EventAuctionRedisMessage message = new EventAuctionRedisMessage(itemId, payload);
                String json = objectMapper.writeValueAsString(message);
                redisTemplate.convertAndSend(EVENT_AUCTION_CHANNEL, json);
                log.debug("Published event-auction bid to Redis channel {}: itemId={}", EVENT_AUCTION_CHANNEL, itemId);
                return;
            }
        } catch (Exception e) {
            log.warn("Redis publish failed for event-auction itemId={}, falling back to local STOMP: {}", itemId, e.getMessage());
        }

        // Fallback: local in-memory broadcast
        if (localMessagingTemplate != null && itemId != null) {
            localMessagingTemplate.convertAndSend("/topic/auction/" + itemId, payload);
        }
    }
}
