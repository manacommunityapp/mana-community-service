package com.manacommunity.platform.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import static com.manacommunity.platform.websocket.SportsWebSocketRedisConfig.MATCH_EVENTS_CHANNEL;

@Slf4j
@Component
public class SportsMatchRedisPublisher {

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate localMessagingTemplate;
    private final ObjectMapper objectMapper;

    public SportsMatchRedisPublisher(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            SimpMessagingTemplate localMessagingTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.localMessagingTemplate = localMessagingTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishEvent(Object eventPayload, Long matchId) {
        try {
            String json = objectMapper.writeValueAsString(eventPayload);
            if (redisTemplate != null) {
                redisTemplate.convertAndSend(MATCH_EVENTS_CHANNEL, json);
                log.debug("Published match event to Redis channel {}: matchId={}", MATCH_EVENTS_CHANNEL, matchId);
                return;
            }
        } catch (Exception e) {
            log.warn("Redis publish failed, falling back to local STOMP broadcast for matchId={}: {}", matchId, e.getMessage());
        }

        // Fallback: local in-memory broadcast
        if (matchId != null) {
            localMessagingTemplate.convertAndSend("/topic/matches/" + matchId + "/live", eventPayload);
            localMessagingTemplate.convertAndSend("/topic/match/" + matchId + "/state", eventPayload);
            localMessagingTemplate.convertAndSend("/topic/match/" + matchId + "/generic-state", eventPayload);
        }
    }
}
