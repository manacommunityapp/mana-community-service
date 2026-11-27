package com.manacommunity.platform.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import static com.manacommunity.platform.websocket.SportsWebSocketRedisConfig.GROUP_ORDER_CHANNEL;

/**
 * Publishes group-order status events to the Redis Pub/Sub channel
 * {@value SportsWebSocketRedisConfig#GROUP_ORDER_CHANNEL}.
 *
 * <p>Falls back to a direct local STOMP broadcast when Redis is unavailable.</p>
 */
@Slf4j
@Component
public class GroupOrderRedisPublisher {

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate localMessagingTemplate;
    private final ObjectMapper objectMapper;

    public GroupOrderRedisPublisher(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Autowired(required = false) SimpMessagingTemplate localMessagingTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.localMessagingTemplate = localMessagingTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes a group-order status event.
     *
     * @param communityId the community ID (routed to {@code /topic/community/{id}/group-orders})
     * @param payload     the event payload map
     */
    public void publishStatusEvent(Long communityId, Object payload) {
        try {
            if (redisTemplate != null) {
                GroupOrderRedisMessage message = new GroupOrderRedisMessage(communityId, payload);
                String json = objectMapper.writeValueAsString(message);
                redisTemplate.convertAndSend(GROUP_ORDER_CHANNEL, json);
                log.debug("Published group-order status event to Redis channel {}: communityId={}", GROUP_ORDER_CHANNEL, communityId);
                return;
            }
        } catch (Exception e) {
            log.warn("Redis publish failed for group-order communityId={}, falling back to local STOMP: {}", communityId, e.getMessage());
        }

        // Fallback: local in-memory broadcast
        if (localMessagingTemplate != null && communityId != null) {
            localMessagingTemplate.convertAndSend("/topic/community/" + communityId + "/group-orders", payload);
        }
    }
}
