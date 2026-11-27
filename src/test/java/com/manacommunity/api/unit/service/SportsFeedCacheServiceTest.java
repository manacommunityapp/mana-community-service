package com.manacommunity.api.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.sports.dto.SportsFeedMatchResponse;
import com.manacommunity.api.sports.service.SportsFeedCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Sports Feed Cache Service Tests")
class SportsFeedCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SportsFeedCacheService cacheService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheService = new SportsFeedCacheService(redisTemplate, objectMapper);
    }

    @Test
    @DisplayName("getLiveFeed returns cached JSON without invoking DB loader on cache HIT")
    void getLiveFeedCacheHit() throws Exception {
        Long communityId = 2L;
        String json = "[{\"matchId\":101,\"eventName\":\"Team A vs Team B\",\"status\":\"LIVE\"}]";
        when(valueOperations.get("sports:feed:version")).thenReturn("1");
        when(valueOperations.get("sports:feed:live-strip:v1:community:2")).thenReturn(json);

        AtomicBoolean dbCalled = new AtomicBoolean(false);
        List<SportsFeedMatchResponse> result = cacheService.getLiveFeed(communityId, () -> {
            dbCalled.set(true);
            return List.of();
        });

        assertThat(result).hasSize(1);
        assertThat(result.get(0).matchId()).isEqualTo(101L);
        assertThat(dbCalled.get()).isFalse();
    }

    @Test
    @DisplayName("getLiveFeed invokes DB loader and caches result on cache MISS")
    void getLiveFeedCacheMiss() {
        Long communityId = 2L;
        when(valueOperations.get("sports:feed:version")).thenReturn("1");
        when(valueOperations.get("sports:feed:live-strip:v1:community:2")).thenReturn(null);

        SportsFeedMatchResponse match = sampleMatch(102L, "Semifinal", "LIVE");

        AtomicBoolean dbCalled = new AtomicBoolean(false);
        List<SportsFeedMatchResponse> result = cacheService.getLiveFeed(communityId, () -> {
            dbCalled.set(true);
            return List.of(match);
        });

        assertThat(result).hasSize(1);
        assertThat(dbCalled.get()).isTrue();
        verify(valueOperations).set(eq("sports:feed:live-strip:v1:community:2"), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("getUpcomingFeed returns cached JSON without invoking DB loader on cache HIT")
    void getUpcomingFeedCacheHit() throws Exception {
        Long communityId = 2L;
        int limit = 15;
        String json = "[{\"matchId\":201,\"eventName\":\"Match 1\",\"status\":\"SCHEDULED\"}]";
        when(valueOperations.get("sports:feed:version")).thenReturn("1");
        when(valueOperations.get("sports:feed:upcoming:v1:community:2:limit:15")).thenReturn(json);

        AtomicBoolean dbCalled = new AtomicBoolean(false);
        List<SportsFeedMatchResponse> result = cacheService.getUpcomingFeed(communityId, limit, () -> {
            dbCalled.set(true);
            return List.of();
        });

        assertThat(result).hasSize(1);
        assertThat(result.get(0).matchId()).isEqualTo(201L);
        assertThat(dbCalled.get()).isFalse();
    }

    @Test
    @DisplayName("getUpcomingFeed invokes DB loader and caches on cache MISS")
    void getUpcomingFeedCacheMiss() {
        Long communityId = 2L;
        int limit = 15;
        when(valueOperations.get("sports:feed:version")).thenReturn("1");
        when(valueOperations.get("sports:feed:upcoming:v1:community:2:limit:15")).thenReturn(null);

        SportsFeedMatchResponse match = sampleMatch(202L, "Final Match", "SCHEDULED");

        AtomicBoolean dbCalled = new AtomicBoolean(false);
        List<SportsFeedMatchResponse> result = cacheService.getUpcomingFeed(communityId, limit, () -> {
            dbCalled.set(true);
            return List.of(match);
        });

        assertThat(result).hasSize(1);
        assertThat(dbCalled.get()).isTrue();
        verify(valueOperations).set(eq("sports:feed:upcoming:v1:community:2:limit:15"), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("invalidateLiveFeed increments version in Redis")
    void invalidateLiveFeed() {
        cacheService.invalidateLiveFeed();
        verify(valueOperations).increment("sports:feed:version");
    }

    @Test
    @DisplayName("Graceful fallback when Redis is null")
    void fallbackWhenRedisNull() {
        SportsFeedCacheService nullCache = new SportsFeedCacheService(null, objectMapper);
        List<SportsFeedMatchResponse> result = nullCache.getLiveFeed(1L, () -> List.of(
                sampleMatch(999L, "Fallback Match", "LIVE")
        ));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).matchId()).isEqualTo(999L);
    }

    private static SportsFeedMatchResponse sampleMatch(Long matchId, String eventName, String status) {
        return new SportsFeedMatchResponse(
                matchId, 10L, "Cricket", "Premier Cup", eventName, "Knockout",
                "Team A", "Team B", "A1", "B1", "100/2", "90/4", status,
                null, null, null, "Ground 1"
        );
    }
}
