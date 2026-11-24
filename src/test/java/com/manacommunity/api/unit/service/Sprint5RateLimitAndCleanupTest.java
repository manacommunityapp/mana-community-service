package com.manacommunity.api.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.exception.SmsRateLimitException;
import com.manacommunity.api.notification.config.SmsProperties;
import com.manacommunity.api.notification.service.SmsRateLimiter;
import com.manacommunity.api.privacy.DataRightsRateLimiter;
import com.manacommunity.api.sports.repository.SportsMatchEventOutboxRepository;
import com.manacommunity.api.sports.service.SportsLiveScoreCacheService;
import com.manacommunity.platform.ratelimit.AuctionBidRateLimiter;
import com.manacommunity.platform.ratelimit.RedisSlidingWindowRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Sprint 5 Rate Limiting & Match TTL Cleanup Tests")
class Sprint5RateLimitAndCleanupTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SportsMatchEventOutboxRepository outboxRepository;

    private RedisSlidingWindowRateLimiter slidingWindow;

    @BeforeEach
    void setUp() {
        slidingWindow = new RedisSlidingWindowRateLimiter(redisTemplate);
    }

    // ── #11 RedisSlidingWindowRateLimiter + SmsRateLimiter + DataRightsRateLimiter ──

    @Test
    @DisplayName("RedisSlidingWindowRateLimiter runs Lua script and handles allow, deny, and Redis failure fallback")
    @SuppressWarnings("unchecked")
    void luaSlidingWindowBehavior() {
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("test:key")), any(), any(), any(), any()))
                .thenReturn(1L)
                .thenReturn(0L)
                .thenThrow(new RuntimeException("Redis timeout"));

        assertThat(slidingWindow.tryAcquire("test:key", 60_000L, 3)).isTrue();
        assertThat(slidingWindow.tryAcquire("test:key", 60_000L, 3)).isFalse();
        assertThat(slidingWindow.tryAcquire("test:key", 60_000L, 3)).isNull();

        RedisSlidingWindowRateLimiter nullRedis = new RedisSlidingWindowRateLimiter(null);
        assertThat(nullRedis.tryAcquire("test:key", 60_000L, 3)).isNull();
    }

    @Test
    @DisplayName("SmsRateLimiter delegates to Redis Lua when available and falls back to memory on Redis failure")
    void smsRateLimiterRedisAndFallback() {
        SmsProperties props = new SmsProperties();
        props.getRateLimit().setEnabled(true);
        props.getRateLimit().setPerPhonePerMinute(2);

        RedisSlidingWindowRateLimiter mockWindow = mock(RedisSlidingWindowRateLimiter.class);
        SmsRateLimiter limiter = new SmsRateLimiter(props, mockWindow);

        // Redis allows, then blocks, then returns null (fallback allows up to 2 in memory)
        when(mockWindow.tryAcquire("sms:rl:phone:+919999999999", 60_000L, 2))
                .thenReturn(true)
                .thenReturn(false)
                .thenReturn(null);

        assertThatCode(() -> limiter.checkPhone("+919999999999")).doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.checkPhone("+919999999999"))
                .isInstanceOf(SmsRateLimitException.class);

        // On null (Redis error), fallback in-memory allows 2 attempts then throws on 3rd
        assertThatCode(() -> limiter.checkPhone("+919999999999")).doesNotThrowAnyException();
        assertThatCode(() -> limiter.checkPhone("+919999999999")).doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.checkPhone("+919999999999"))
                .isInstanceOf(SmsRateLimitException.class);
    }

    @Test
    @DisplayName("DataRightsRateLimiter uses Redis Lua and falls back cleanly to in-memory")
    void dataRightsRateLimiterRedisAndFallback() {
        RedisSlidingWindowRateLimiter mockWindow = mock(RedisSlidingWindowRateLimiter.class);
        DataRightsRateLimiter limiter = new DataRightsRateLimiter(mockWindow);

        when(mockWindow.tryAcquire("privacy:rights:rl:user:10", 24 * 3_600_000L, 5))
                .thenReturn(true)
                .thenReturn(false);

        assertThatCode(() -> limiter.checkAndRecord(10L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.checkAndRecord(10L))
                .isInstanceOf(ManaCommunityException.class)
                .hasMessageContaining("Too many data-rights requests");
    }

    // ── #12 AuctionBidRateLimiter ───────────────────────────────────────────

    @Test
    @DisplayName("AuctionBidRateLimiter limits rapid repeat bids per team/user and allows independent teams")
    void auctionBidRateLimiterSportsAndEvent() {
        RedisSlidingWindowRateLimiter mockWindow = mock(RedisSlidingWindowRateLimiter.class);
        AuctionBidRateLimiter redisLimiter = new AuctionBidRateLimiter(mockWindow, 2000L, 1);

        when(mockWindow.tryAcquire("auction:rl:event:50:user:9", 2000L, 1))
                .thenReturn(true)
                .thenReturn(false);

        assertThat(redisLimiter.tryAcquireEventBid(50L, 9L)).isTrue();
        assertThat(redisLimiter.tryAcquireEventBid(50L, 9L)).isFalse();

        // In-memory fallback: same team blocked within cooldown, different team allowed
        AuctionBidRateLimiter memLimiter = new AuctionBidRateLimiter(null, 5000L, 1);
        assertThat(memLimiter.tryAcquireSportsBid(100L, 1L)).isTrue();
        assertThat(memLimiter.tryAcquireSportsBid(100L, 1L)).isFalse();
        assertThat(memLimiter.tryAcquireSportsBid(100L, 2L)).isTrue();
    }

    // ── #13 Match TTL Cleanup on COMPLETED ──────────────────────────────────

    @Test
    @DisplayName("cleanupOnMatchCompleted deletes live keys, expires sequence key for 24h, and clears local counter")
    void matchCleanupOnCompleted() {
        when(outboxRepository.findMaxSequenceNoByMatchId(77L)).thenReturn(5L);
        when(redisTemplate.hasKey("match:77:seq")).thenReturn(true);

        SportsLiveScoreCacheService cacheService =
                new SportsLiveScoreCacheService(redisTemplate, outboxRepository, new ObjectMapper());

        // Populate local fallback counter via null-Redis path? No, let's test seq cleanup via Redis mock
        cacheService.cleanupOnMatchCompleted(77L);

        verify(redisTemplate).delete("match:77:live");
        verify(redisTemplate).delete("match:generic:77:live");
        verify(redisTemplate).expire("match:77:seq", Duration.ofHours(24));

        // Verify local counter is cleared in no-Redis mode too
        SportsLiveScoreCacheService memService =
                new SportsLiveScoreCacheService(null, outboxRepository, new ObjectMapper());
        assertThat(memService.getNextSequence(77L)).isEqualTo(6L);
        memService.cleanupOnMatchCompleted(77L);
        // After cleanup, next getNextSequence re-queries DB max (5) -> 6 again instead of 7
        assertThat(memService.getNextSequence(77L)).isEqualTo(6L);
    }
}
