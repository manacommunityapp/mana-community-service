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
 * Subscribes to the Redis {@value SportsWebSocketRedisConfig#GROUP_ORDER_CHANNEL} channel
 * and forwards events to local WebSocket subscribers on
 * {@code /topic/community/{communityId}/group-orders}.
 */
@Slf4j
@Component
public class GroupOrderRedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public GroupOrderRedisSubscriber(
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
                log.warn("Discarding malformed group-order redis message (missing communityId): {}", json);
                return;
            }
            Long communityId = root.get("communityId").asLong();
            JsonNode payload = root.has("payload") ? root.get("payload") : root;

            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/community/" + communityId + "/group-orders", payload);
                log.debug("Distributed group-order status event for communityId={} to local STOMP subscribers", communityId);
            }
        } catch (Exception e) {
            log.error("Failed to process redis group-order event", e);
        }
    }
}
