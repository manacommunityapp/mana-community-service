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
 * Subscribes to the Redis {@value SportsWebSocketRedisConfig#EVENT_AUCTION_CHANNEL} channel
 * and forwards bid events to local WebSocket subscribers on {@code /topic/auction/{itemId}}.
 */
@Slf4j
@Component
public class EventAuctionRedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public EventAuctionRedisSubscriber(
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

            if (!root.has("itemId")) {
                log.warn("Discarding malformed event-auction message (missing itemId): {}", json);
                return;
            }
            Long itemId = root.get("itemId").asLong();
            JsonNode payload = root.has("payload") ? root.get("payload") : root;

            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/auction/" + itemId, payload);
                log.debug("Distributed event-auction bid for itemId={} to local STOMP subscribers", itemId);
            }
        } catch (Exception e) {
            log.error("Failed to process redis event-auction message", e);
        }
    }
}
