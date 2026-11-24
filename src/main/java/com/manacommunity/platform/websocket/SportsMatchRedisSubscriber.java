package com.manacommunity.platform.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class SportsMatchRedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(json);
            if (!root.has("matchId")) {
                log.warn("Discarding malformed sports match event: {}", json);
                return;
            }
            Long matchId = root.get("matchId").asLong();

            // 1. Broadcast delta event to new primary topic
            messagingTemplate.convertAndSend("/topic/matches/" + matchId + "/live", root);

            // 2. Backwards compatibility: if lastBall or ball event is present, publish to /topic/match/{id}
            if (root.has("lastBall") && !root.get("lastBall").isNull()) {
                messagingTemplate.convertAndSend("/topic/match/" + matchId, root.get("lastBall"));
            }

            // 3. Backwards compatibility: publish to /topic/match/{id}/state
            messagingTemplate.convertAndSend("/topic/match/" + matchId + "/state", root);

            // 4. Generic match state: publish to /topic/match/{id}/generic-state
            if (root.has("scoringConfig") || root.has("periods") || root.has("scoreTeamA")) {
                messagingTemplate.convertAndSend("/topic/match/" + matchId + "/generic-state", root);
            }

            log.debug("Distributed sports match event for matchId={} to local STOMP subscribers", matchId);
        } catch (Exception e) {
            log.error("Failed to process redis match event", e);
        }
    }
}
