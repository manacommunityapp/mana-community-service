package com.manacommunity.api.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.sports.dto.SportsGenericMatchStateResponse;
import com.manacommunity.api.sports.dto.SportsLiveMatchStateResponse;
import com.manacommunity.api.sports.repository.SportsMatchEventOutboxRepository;
import com.manacommunity.api.sports.service.SportsLiveScoreCacheService;
import com.manacommunity.platform.websocket.SportsMatchRedisSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Sports Live Score Cache & Redis Subscriber Tests")
class SportsLiveScoreCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SportsMatchEventOutboxRepository outboxRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SportsLiveScoreCacheService cacheService;
    private SportsMatchRedisSubscriber redisSubscriber;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheService = new SportsLiveScoreCacheService(redisTemplate, outboxRepository, objectMapper);
        redisSubscriber = new SportsMatchRedisSubscriber(messagingTemplate, objectMapper);
    }

    @Test
    @DisplayName("Generic match state cache hit returns cached snapshot without DB loader")
    void genericMatchCacheHit() throws Exception {
        Long matchId = 201L;
        SportsGenericMatchStateResponse cached = new SportsGenericMatchStateResponse(
                matchId, "LIVE", "FOOTBALL", 1L, "Team A", "#FF0000",
                2L, "Team B", "#0000FF", 1, 2, 1, 0, 0,
                List.of(), List.of(), null
        );
        String json = objectMapper.writeValueAsString(cached);
        when(valueOperations.get("match:generic:201:live")).thenReturn(json);

        SportsGenericMatchStateResponse result = cacheService.getGenericLiveSnapshot(matchId, () -> {
            throw new AssertionError("DB loader should not be called on cache hit");
        });

        assertThat(result).isNotNull();
        assertThat(result.matchId()).isEqualTo(201L);
        assertThat(result.sportType()).isEqualTo("FOOTBALL");
        assertThat(result.scoreTeamA()).isEqualTo(2);
        assertThat(result.scoreTeamB()).isEqualTo(1);
    }

    @Test
    @DisplayName("Generic match state cache miss calls DB loader and populates Redis")
    void genericMatchCacheMiss() throws Exception {
        Long matchId = 202L;
        when(valueOperations.get("match:generic:202:live")).thenReturn(null);

        SportsGenericMatchStateResponse dbResponse = new SportsGenericMatchStateResponse(
                matchId, "LIVE", "BADMINTON", 3L, "Player 1", null,
                4L, "Player 2", null, 2, 21, 19, 1, 0,
                List.of(), List.of(), null
        );

        SportsGenericMatchStateResponse result = cacheService.getGenericLiveSnapshot(matchId, () -> dbResponse);

        assertThat(result).isNotNull();
        assertThat(result.matchId()).isEqualTo(202L);
        assertThat(result.sportType()).isEqualTo("BADMINTON");
        verify(valueOperations).set(eq("match:generic:202:live"), anyString(), eq(Duration.ofHours(24)));
    }

    @Test
    @DisplayName("Invalidate removes both cricket and generic cache keys")
    void invalidateClearsBothKeys() {
        cacheService.invalidate(500L);
        verify(redisTemplate).delete("match:500:live");
        verify(redisTemplate).delete("match:generic:500:live");
    }

    @Test
    @DisplayName("Subscriber forwards generic match state to generic-state, live and state topics")
    void subscriberForwardsGenericMatchTopics() {
        String genericJson = """
                {
                    "matchId": 301,
                    "sportType": "FOOTBALL",
                    "status": "LIVE",
                    "scoreTeamA": 1,
                    "scoreTeamB": 0,
                    "currentPeriod": 1,
                    "periods": []
                }
                """;
        Message message = new DefaultMessage("sports:match:events".getBytes(StandardCharsets.UTF_8), genericJson.getBytes(StandardCharsets.UTF_8));

        redisSubscriber.onMessage(message, null);

        verify(messagingTemplate).convertAndSend(eq("/topic/matches/301/live"), any(Object.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/match/301/state"), any(Object.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/match/301/generic-state"), any(Object.class));
    }
}
