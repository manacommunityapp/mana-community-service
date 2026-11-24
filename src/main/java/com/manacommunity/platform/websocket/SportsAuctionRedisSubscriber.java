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

@Slf4j
@Component
public class SportsAuctionRedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public SportsAuctionRedisSubscriber(
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
            if (!root.has("configId")) {
                log.warn("Discarding malformed sports auction event: {}", json);
                return;
            }
            Long configId = root.get("configId").asLong();
            JsonNode payload = root.has("payload") ? root.get("payload") : root;

            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/auction/" + configId, payload);
                log.debug("Distributed sports auction event for configId={} to local STOMP subscribers", configId);
            }
        } catch (Exception e) {
            log.error("Failed to process redis auction event", e);
        }
    }
}
