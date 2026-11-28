package com.manacommunity.platform.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import static com.manacommunity.platform.websocket.SportsWebSocketRedisConfig.AUCTION_EVENTS_CHANNEL;

@Slf4j
@Component
public class SportsAuctionRedisPublisher {

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate localMessagingTemplate;
    private final ObjectMapper objectMapper;

    public SportsAuctionRedisPublisher(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Autowired(required = false) SimpMessagingTemplate localMessagingTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.localMessagingTemplate = localMessagingTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishEvent(Long configId, Object eventPayload) {
        try {
            if (redisTemplate != null) {
                SportsAuctionRedisMessage message = new SportsAuctionRedisMessage(configId, eventPayload);
                String json = objectMapper.writeValueAsString(message);
                redisTemplate.convertAndSend(AUCTION_EVENTS_CHANNEL, json);
                log.debug("Published auction event to Redis channel {}: configId={}", AUCTION_EVENTS_CHANNEL, configId);
                return;
            }
        } catch (Exception e) {
            log.warn("Redis publish failed, falling back to local STOMP broadcast for auction configId={}: {}", configId, e.getMessage());
        }

        // Fallback: local in-memory broadcast
        if (configId != null && localMessagingTemplate != null) {
            localMessagingTemplate.convertAndSend("/topic/auction/" + configId, eventPayload);
        }
    }
}
