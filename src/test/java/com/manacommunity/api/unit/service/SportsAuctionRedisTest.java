package com.manacommunity.api.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.manacommunity.api.sports.dto.PlayerWithBidResponse;
import com.manacommunity.api.sports.dto.SportsAuctionBidResponse;
import com.manacommunity.api.sports.service.SportsAuctionCacheService;
import com.manacommunity.api.sports.service.SportsAuctionWebSocketService;
import com.manacommunity.platform.websocket.SportsAuctionRedisPublisher;
import com.manacommunity.platform.websocket.SportsAuctionRedisSubscriber;
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
import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Sports Auction Redis Publisher, Subscriber & Cache Tests")
class SportsAuctionRedisTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private ObjectMapper objectMapper;
    private SportsAuctionCacheService cacheService;
    private SportsAuctionRedisPublisher redisPublisher;
    private SportsAuctionRedisSubscriber redisSubscriber;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheService = new SportsAuctionCacheService(redisTemplate, objectMapper);
        redisPublisher = new SportsAuctionRedisPublisher(redisTemplate, messagingTemplate, objectMapper);
        redisSubscriber = new SportsAuctionRedisSubscriber(messagingTemplate, objectMapper);
    }

    @Test
    @DisplayName("Auction current player cache hit returns snapshot without DB loader")
    void currentPlayerCacheHit() throws Exception {
        Long configId = 101L;
        PlayerWithBidResponse cached = new PlayerWithBidResponse(
                55L, "Player Alpha", "BATSMAN", "Top Order", 25, 10000,
                "{}", 15000L, 16000L, 1000, "Team Lions", 1, "SELLING",
                null, null, null, null, null
        );
        String json = objectMapper.writeValueAsString(cached);
        when(valueOperations.get("auction:101:current-player")).thenReturn(json);

        PlayerWithBidResponse result = cacheService.getCurrentPlayer(configId, () -> {
            throw new AssertionError("DB loader should not be invoked on cache hit");
        });

        assertThat(result).isNotNull();
        assertThat(result.playerId()).isEqualTo(55L);
        assertThat(result.playerName()).isEqualTo("Player Alpha");
        assertThat(result.currentBid()).isEqualTo(15000L);
    }

    @Test
    @DisplayName("Auction current player cache miss calls DB loader and populates Redis")
    void currentPlayerCacheMiss() {
        Long configId = 102L;
        when(valueOperations.get("auction:102:current-player")).thenReturn(null);

        PlayerWithBidResponse dbPlayer = new PlayerWithBidResponse(
                56L, "Player Beta", "BOWLER", "Pacer", 28, 20000,
                "{}", 20000L, 22000L, 2000, null, 2, "SELLING",
                null, null, null, null, null
        );

        PlayerWithBidResponse result = cacheService.getCurrentPlayer(configId, () -> dbPlayer);

        assertThat(result).isNotNull();
        assertThat(result.playerId()).isEqualTo(56L);
        verify(valueOperations).set(eq("auction:102:current-player"), anyString(), eq(Duration.ofHours(2)));
    }

    @Test
    @DisplayName("Invalidating auction cache deletes the current player key")
    void invalidateCache() {
        cacheService.invalidate(103L);
        verify(redisTemplate).delete("auction:103:current-player");
    }

    @Test
    @DisplayName("Publisher sends JSON message with configId to Redis auction channel")
    void publisherSendsToRedis() throws Exception {
        Long configId = 10L;
        SportsAuctionBidResponse bid = SportsAuctionBidResponse.builder()
                .id(1L).configId(configId).playerId(55L).teamId(2L)
                .teamName("Team Titans").bidAmount(50000L).build();
        SportsAuctionWebSocketService.AuctionEvent event =
                new SportsAuctionWebSocketService.AuctionEvent("BID_PLACED", bid);

        redisPublisher.publishEvent(configId, event);

        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(channelCaptor.capture(), messageCaptor.capture());

        assertThat(channelCaptor.getValue()).isEqualTo("sports:auction:events");
        assertThat(messageCaptor.getValue()).contains("\"configId\":10");
        assertThat(messageCaptor.getValue()).contains("BID_PLACED");
    }

    @Test
    @DisplayName("Subscriber forwards Redis auction event to local STOMP /topic/auction/{configId}")
    void subscriberForwardsToStomp() {
        String json = """
                {
                    "configId": 88,
                    "payload": {
                        "type": "BID_PLACED",
                        "payload": {
                            "bidAmount": 75000,
                            "teamName": "Warriors"
                        }
                    }
                }
                """;
        Message message = new DefaultMessage("sports:auction:events".getBytes(StandardCharsets.UTF_8), json.getBytes(StandardCharsets.UTF_8));

        redisSubscriber.onMessage(message, null);

        verify(messagingTemplate).convertAndSend(eq("/topic/auction/88"), any(Object.class));
    }
}
