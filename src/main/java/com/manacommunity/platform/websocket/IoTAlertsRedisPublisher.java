package com.manacommunity.platform.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import static com.manacommunity.platform.websocket.SportsWebSocketRedisConfig.IOT_ALERTS_CHANNEL;

/**
 * Publishes IoT sensor anomaly alerts to the Redis Pub/Sub channel
 * {@value SportsWebSocketRedisConfig#IOT_ALERTS_CHANNEL}.
 *
 * <p>Falls back to a direct local STOMP broadcast when Redis is unavailable.</p>
 */
@Slf4j
@Component
public class IoTAlertsRedisPublisher {

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate localMessagingTemplate;
    private final ObjectMapper objectMapper;

    public IoTAlertsRedisPublisher(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Autowired(required = false) SimpMessagingTemplate localMessagingTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.localMessagingTemplate = localMessagingTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes a sensor anomaly alert.
     *
     * @param communityId the community ID (routed to {@code /topic/community/{id}/iot-feed})
     * @param payload     the alert payload map
     */
    public void publishAlert(Long communityId, Object payload) {
        try {
            if (redisTemplate != null) {
                IoTAlertRedisMessage message = new IoTAlertRedisMessage(communityId, payload);
                String json = objectMapper.writeValueAsString(message);
                redisTemplate.convertAndSend(IOT_ALERTS_CHANNEL, json);
                log.debug("Published IoT alert to Redis channel {}: communityId={}", IOT_ALERTS_CHANNEL, communityId);
                return;
            }
        } catch (Exception e) {
            log.warn("Redis publish failed for IoT alert communityId={}, falling back to local STOMP: {}", communityId, e.getMessage());
        }

        // Fallback: local in-memory broadcast
        if (localMessagingTemplate != null && communityId != null) {
            localMessagingTemplate.convertAndSend("/topic/community/" + communityId + "/iot-feed", payload);
        }
    }
}
