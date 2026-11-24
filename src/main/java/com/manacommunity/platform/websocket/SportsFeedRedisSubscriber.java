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
 * Subscribes to the Redis {@value SportsWebSocketRedisConfig#FEED_EVENTS_CHANNEL} channel
 * and forwards the event to all local WebSocket subscribers on {@code /topic/sports/live}.
 */
@Slf4j
@Component
public class SportsFeedRedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public SportsFeedRedisSubscriber(
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

            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/sports/live", root);
                log.debug("Distributed sports feed event to local STOMP subscribers on /topic/sports/live");
            }
        } catch (Exception e) {
            log.error("Failed to process redis feed event", e);
        }
    }
}
