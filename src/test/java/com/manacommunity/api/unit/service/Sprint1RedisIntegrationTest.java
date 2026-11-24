package com.manacommunity.api.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.manacommunity.api.security.TokenBlacklistService;
import com.manacommunity.platform.websocket.EventAuctionRedisPublisher;
import com.manacommunity.platform.websocket.EventAuctionRedisSubscriber;
import com.manacommunity.platform.websocket.GroupOrderRedisPublisher;
import com.manacommunity.platform.websocket.GroupOrderRedisSubscriber;
import com.manacommunity.platform.websocket.IoTAlertsRedisPublisher;
import com.manacommunity.platform.websocket.IoTAlertsRedisSubscriber;
import com.manacommunity.platform.websocket.SportsFeedRedisPublisher;
import com.manacommunity.platform.websocket.SportsFeedRedisSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Sprint 1 Critical Redis Pub/Sub & Token Blacklist Tests")
class Sprint1RedisIntegrationTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ── TokenBlacklistService ───────────────────────────────────────────────

    @Test
    @DisplayName("TokenBlacklistService stores jti in Redis with remaining TTL and checks hasKey")
    void tokenBlacklistUsesRedisWhenAvailable() {
        TokenBlacklistService service = new TokenBlacklistService(redisTemplate);
        long futureExpiry = System.currentTimeMillis() + 60_000L;

        service.blacklist("jti-abc-123", futureExpiry);

        verify(valueOperations).set(eq("token:blacklist:jti-abc-123"), eq("1"), longThat(ttl -> ttl > 0 && ttl <= 60_000L), eq(TimeUnit.MILLISECONDS));

        when(redisTemplate.hasKey("token:blacklist:jti-abc-123")).thenReturn(true);
        assertThat(service.isBlacklisted("jti-abc-123")).isTrue();

        when(redisTemplate.hasKey("token:blacklist:unknown")).thenReturn(false);
        assertThat(service.isBlacklisted("unknown")).isFalse();
    }

    @Test
    @DisplayName("TokenBlacklistService falls back to in-memory store when Redis is null")
    void tokenBlacklistFallsBackToMemoryWhenRedisUnavailable() {
        TokenBlacklistService service = new TokenBlacklistService(null);
        long futureExpiry = System.currentTimeMillis() + 60_000L;

        service.blacklist("jti-mem-1", futureExpiry);
        assertThat(service.isBlacklisted("jti-mem-1")).isTrue();
        assertThat(service.isBlacklisted("jti-other")).isFalse();

        // Already expired token should not be stored or reported as blacklisted
        service.blacklist("jti-expired", System.currentTimeMillis() - 1000L);
        assertThat(service.isBlacklisted("jti-expired")).isFalse();
    }

    @Test
    @DisplayName("TokenBlacklistService falls back to in-memory if Redis throws an exception")
    void tokenBlacklistFallsBackOnRedisError() {
        TokenBlacklistService service = new TokenBlacklistService(redisTemplate);
        doThrow(new RuntimeException("Redis down")).when(valueOperations).set(anyString(), anyString(), anyLong(), any());
        when(redisTemplate.hasKey("token:blacklist:jti-err-1")).thenThrow(new RuntimeException("Redis down"));

        long futureExpiry = System.currentTimeMillis() + 60_000L;
        service.blacklist("jti-err-1", futureExpiry);
        assertThat(service.isBlacklisted("jti-err-1")).isTrue();
    }

    // ── SportsFeed Redis Pub/Sub ────────────────────────────────────────────

    @Test
    @DisplayName("SportsFeedRedisPublisher publishes to sports:feed:events and subscriber forwards to /topic/sports/live")
    void sportsFeedPubSubRoundTrip() {
        SportsFeedRedisPublisher publisher = new SportsFeedRedisPublisher(redisTemplate, messagingTemplate, objectMapper);
        SportsFeedRedisSubscriber subscriber = new SportsFeedRedisSubscriber(messagingTemplate, objectMapper);

        publisher.publishFeedRefresh(Map.of("type", "REFRESH"));

        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(channelCaptor.capture(), msgCaptor.capture());

        assertThat(channelCaptor.getValue()).isEqualTo("sports:feed:events");
        assertThat(msgCaptor.getValue()).contains("\"type\":\"REFRESH\"");

        Message redisMsg = new DefaultMessage(
                "sports:feed:events".getBytes(StandardCharsets.UTF_8),
                msgCaptor.getValue().getBytes(StandardCharsets.UTF_8));
        subscriber.onMessage(redisMsg, null);

        verify(messagingTemplate).convertAndSend(eq("/topic/sports/live"), any(Object.class));
    }

    @Test
    @DisplayName("SportsFeedRedisPublisher falls back to local STOMP when Redis is null")
    void sportsFeedFallsBackWhenRedisNull() {
        SportsFeedRedisPublisher publisher = new SportsFeedRedisPublisher(null, messagingTemplate, objectMapper);
        Map<String, String> payload = Map.of("type", "REFRESH");

        publisher.publishFeedRefresh(payload);

        verify(messagingTemplate).convertAndSend(eq("/topic/sports/live"), eq(payload));
    }

    // ── EventAuction Redis Pub/Sub ──────────────────────────────────────────

    @Test
    @DisplayName("EventAuctionRedisPublisher publishes to community:event-auction:events and subscriber forwards to /topic/auction/{itemId}")
    void eventAuctionPubSubRoundTrip() {
        EventAuctionRedisPublisher publisher = new EventAuctionRedisPublisher(redisTemplate, messagingTemplate, objectMapper);
        EventAuctionRedisSubscriber subscriber = new EventAuctionRedisSubscriber(messagingTemplate, objectMapper);

        publisher.publishBidEvent(42L, Map.of("type", "BID_PLACED", "amount", 500));

        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(channelCaptor.capture(), msgCaptor.capture());

        assertThat(channelCaptor.getValue()).isEqualTo("community:event-auction:events");
        assertThat(msgCaptor.getValue()).contains("\"itemId\":42");

        Message redisMsg = new DefaultMessage(
                "community:event-auction:events".getBytes(StandardCharsets.UTF_8),
                msgCaptor.getValue().getBytes(StandardCharsets.UTF_8));
        subscriber.onMessage(redisMsg, null);

        verify(messagingTemplate).convertAndSend(eq("/topic/auction/42"), any(Object.class));
    }

    // ── GroupOrder Redis Pub/Sub ────────────────────────────────────────────

    @Test
    @DisplayName("GroupOrderRedisPublisher publishes to community:grouporder:events and subscriber forwards to /topic/community/{id}/group-orders")
    void groupOrderPubSubRoundTrip() {
        GroupOrderRedisPublisher publisher = new GroupOrderRedisPublisher(redisTemplate, messagingTemplate, objectMapper);
        GroupOrderRedisSubscriber subscriber = new GroupOrderRedisSubscriber(messagingTemplate, objectMapper);

        publisher.publishStatusEvent(7L, Map.of("groupOrderId", 99L, "status", "MIN_REACHED"));

        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(channelCaptor.capture(), msgCaptor.capture());

        assertThat(channelCaptor.getValue()).isEqualTo("community:grouporder:events");
        assertThat(msgCaptor.getValue()).contains("\"communityId\":7");

        Message redisMsg = new DefaultMessage(
                "community:grouporder:events".getBytes(StandardCharsets.UTF_8),
                msgCaptor.getValue().getBytes(StandardCharsets.UTF_8));
        subscriber.onMessage(redisMsg, null);

        verify(messagingTemplate).convertAndSend(eq("/topic/community/7/group-orders"), any(Object.class));
    }

    // ── IoTAlerts Redis Pub/Sub ─────────────────────────────────────────────

    @Test
    @DisplayName("IoTAlertsRedisPublisher publishes to community:iot:alerts and subscriber forwards to /topic/community/{id}/iot-feed")
    void iotAlertsPubSubRoundTrip() {
        IoTAlertsRedisPublisher publisher = new IoTAlertsRedisPublisher(redisTemplate, messagingTemplate, objectMapper);
        IoTAlertsRedisSubscriber subscriber = new IoTAlertsRedisSubscriber(messagingTemplate, objectMapper);

        publisher.publishAlert(12L, Map.of("type", "ANOMALY", "sensorId", 5L, "severity", "HIGH"));

        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(channelCaptor.capture(), msgCaptor.capture());

        assertThat(channelCaptor.getValue()).isEqualTo("community:iot:alerts");
        assertThat(msgCaptor.getValue()).contains("\"communityId\":12");

        Message redisMsg = new DefaultMessage(
                "community:iot:alerts".getBytes(StandardCharsets.UTF_8),
                msgCaptor.getValue().getBytes(StandardCharsets.UTF_8));
        subscriber.onMessage(redisMsg, null);

        verify(messagingTemplate).convertAndSend(eq("/topic/community/12/iot-feed"), any(Object.class));
    }
}
