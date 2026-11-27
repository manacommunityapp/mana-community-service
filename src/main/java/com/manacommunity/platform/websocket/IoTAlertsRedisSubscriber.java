package com.manacommunity.platform.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Subscribes to the Redis {@value SportsWebSocketRedisConfig#IOT_ALERTS_CHANNEL} channel
 * and forwards alerts to local WebSocket subscribers on
 * {@code /topic/community/{communityId}/iot-feed}.
 */
@Slf4j
@Component
public class IoTAlertsRedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public IoTAlertsRedisSubscriber(
            @Autowired(required = false) SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(json);

            if (!root.has("communityId")) {
                log.warn("Discarding malformed IoT alert redis message (missing communityId): {}", json);
                return;
            }
            Long communityId = root.get("communityId").asLong();
            JsonNode payload = root.has("payload") ? root.get("payload") : root;

            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/community/" + communityId + "/iot-feed", payload);
                log.debug("Distributed IoT alert for communityId={} to local STOMP subscribers", communityId);
            }
        } catch (Exception e) {
            log.error("Failed to process redis IoT alert", e);
        }
    }
}
