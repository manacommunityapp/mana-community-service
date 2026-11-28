package com.manacommunity.api.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.manacommunity.api.mobility.service.MobilityDriverLocationCacheService;
import com.manacommunity.api.mobility.service.MobilityDriverLocationCacheService.CachedDriverLocation;
import com.manacommunity.api.sports.dto.SportsAnalyticsResponse;
import com.manacommunity.api.sports.dto.SportsAnalyticsResponse.*;
import com.manacommunity.api.sports.dto.SportsAuctionStatsResponse;
import com.manacommunity.api.sports.dto.SportsDashboardResponse.Stats;
import com.manacommunity.api.sports.dto.SportsDashboardResponse.UpcomingEvent;
import com.manacommunity.api.sports.dto.SportsFeedMatchResponse;
import com.manacommunity.api.sports.service.SportsAnalyticsCacheService;
import com.manacommunity.api.sports.service.SportsAuctionCacheService;
import com.manacommunity.api.sports.service.SportsDashboardCacheService;
import com.manacommunity.api.sports.service.SportsFeedCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Sprint 2 High-Priority Redis Caching Tests")
class Sprint2RedisCachingTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private HashOperations<String, Object, Object> hashOperations;
    @Mock private GeoOperations<String, String> geoOperations;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
    }

    // ── #7 SportsAuction Stats Cache ────────────────────────────────────────

    @Test
    @DisplayName("SportsAuctionCacheService caches and invalidates auction stats with 30s TTL")
    void auctionStatsCacheHitMissInvalidate() throws Exception {
        SportsAuctionCacheService cacheService = new SportsAuctionCacheService(redisTemplate, objectMapper);
        SportsAuctionStatsResponse dbStats = new SportsAuctionStatsResponse(60, 12, 48, 6, 600000L, 120000L);

        // Miss -> DB load -> store 30s TTL
        when(valueOperations.get("auction:15:stats")).thenReturn(null);
        SportsAuctionStatsResponse missResult = cacheService.getAuctionStats(15L, () -> dbStats);
        assertThat(missResult).isEqualTo(dbStats);
        verify(valueOperations).set(eq("auction:15:stats"), anyString(), eq(Duration.ofSeconds(30)));

        // Hit -> no DB load
        when(valueOperations.get("auction:15:stats")).thenReturn(objectMapper.writeValueAsString(dbStats));
        SportsAuctionStatsResponse hitResult = cacheService.getAuctionStats(15L, () -> {
            throw new AssertionError("DB loader should not run on cache hit");
        });
        assertThat(hitResult.soldPlayers()).isEqualTo(12);

        // Invalidate deletes both current-player and stats keys
        cacheService.invalidate(15L);
        verify(redisTemplate).delete("auction:15:current-player");
        verify(redisTemplate).delete("auction:15:stats");
    }

    // ── #6 SportsAnalytics Cache ────────────────────────────────────────────

    @Test
    @DisplayName("SportsAnalyticsCacheService caches versioned keys and invalidates via version increment")
    void analyticsCacheHitMissAndVersionInvalidation() throws Exception {
        SportsAnalyticsCacheService cacheService = new SportsAnalyticsCacheService(redisTemplate, objectMapper);
        SportsAnalyticsResponse sample = new SportsAnalyticsResponse(
                new OverviewMetrics(3, 1, 10, 5, 200, 180, 20, 150, 2),
                List.of(), List.of(),
                new GenderBreakdown(100, 50, 0, 66.7, 33.3),
                new AgeGroupBreakdown(20, 40, 80, 10),
                List.of(), List.of(),
                new AuctionAnalytics(1, 4, 40, 400000, 250000)
        );

        when(valueOperations.get("analytics:version")).thenReturn("3");
        String expectedKey = "analytics:v3:community:9:sport:2:2026-01-01:2026-12-31";
        when(valueOperations.get(expectedKey)).thenReturn(null);

        SportsAnalyticsResponse res = cacheService.getAnalytics(
                9L, 2L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), () -> sample);
        assertThat(res.overview().totalTournaments()).isEqualTo(3);
        verify(valueOperations).set(eq(expectedKey), anyString(), eq(Duration.ofMinutes(15)));

        // Cache hit
        when(valueOperations.get(expectedKey)).thenReturn(objectMapper.writeValueAsString(sample));
        SportsAnalyticsResponse hit = cacheService.getAnalytics(
                9L, 2L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                () -> { throw new AssertionError("Should not hit DB on cache hit"); });
        assertThat(hit.overview().confirmedRegistrations()).isEqualTo(180);

        // Invalidate
        cacheService.invalidateAll();
        verify(valueOperations).increment("analytics:version");
    }

    // ── #8 SportsDashboard Cache ────────────────────────────────────────────

    @Test
    @DisplayName("SportsDashboardCacheService caches upcoming-events and stats with fallback")
    void dashboardCacheHitMissAndFallback() throws Exception {
        SportsDashboardCacheService cacheService = new SportsDashboardCacheService(redisTemplate, objectMapper);
        List<UpcomingEvent> upcoming = List.of(
                new UpcomingEvent(10L, "Mens Singles", "Badminton", "Court 1", "Open",
                        "REGISTRATION_OPEN", LocalDate.of(2026, 12, 1), null, 5L, "Winter Cup")
        );

        when(valueOperations.get("dashboard:version")).thenReturn("1");
        String upcomingKey = "dashboard:v1:user:42:upcoming-events";
        when(valueOperations.get(upcomingKey)).thenReturn(null);

        List<UpcomingEvent> miss = cacheService.getUpcomingEvents(42L, () -> upcoming);
        assertThat(miss).hasSize(1);
        verify(valueOperations).set(eq(upcomingKey), anyString(), eq(Duration.ofMinutes(5)));

        // Stats hit
        Stats stats = new Stats(2, 1, 5, 3);
        String statsKey = "dashboard:v1:stats:user:42:comm:7";
        when(valueOperations.get(statsKey)).thenReturn(objectMapper.writeValueAsString(stats));
        Stats hitStats = cacheService.getStats(42L, false, 7L, () -> {
            throw new AssertionError("Should not hit DB");
        });
        assertThat(hitStats.upcomingTournaments()).isEqualTo(3);

        // Fallback when Redis is null
        SportsDashboardCacheService noRedis = new SportsDashboardCacheService(null, objectMapper);
        assertThat(noRedis.getStats(42L, false, 7L, () -> stats)).isEqualTo(stats);
    }

    // ── #9 SportsFeed Live-Strip Cache ──────────────────────────────────────

    @Test
    @DisplayName("SportsFeedCacheService caches live-strip matches and invalidates on version bump")
    void feedLiveStripCacheAndInvalidation() {
        SportsFeedCacheService cacheService = new SportsFeedCacheService(redisTemplate, objectMapper);
        List<SportsFeedMatchResponse> matches = List.of(
                new SportsFeedMatchResponse(1L, 2L, "Cricket", "Premier League", "Finals", "Final",
                        "Titans", "Warriors", "A101", "B202", "120/3", "118/9", "LIVE",
                        null, null, null, "Main Ground")
        );

        when(valueOperations.get("sports:feed:version")).thenReturn("2");
        String key = "sports:feed:live-strip:v2:community:10";
        when(valueOperations.get(key)).thenReturn(null);

        List<SportsFeedMatchResponse> loaded = cacheService.getLiveFeed(10L, () -> matches);
        assertThat(loaded).hasSize(1);
        verify(valueOperations).set(eq(key), anyString(), eq(Duration.ofSeconds(60)));

        cacheService.invalidateLiveFeed();
        verify(valueOperations).increment("sports:feed:version");
    }

    // ── #10 Mobility Driver Location & GEO Cache ────────────────────────────

    @Test
    @DisplayName("MobilityDriverLocationCacheService caches GPS hash, updates GEO index, and queries nearest drivers")
    void mobilityLocationAndGeoOperations() {
        MobilityDriverLocationCacheService cacheService = new MobilityDriverLocationCacheService(redisTemplate);

        boolean cached = cacheService.updateLocation(88L, 17.4435, 78.3772, true);
        assertThat(cached).isTrue();

        verify(hashOperations).putAll(eq("mobility:driver:88:location"), argThat(m ->
                "17.4435".equals(m.get("lat")) && "78.3772".equals(m.get("lng"))));
        verify(redisTemplate).expire(eq("mobility:driver:88:location"), eq(Duration.ofMinutes(2)));
        verify(geoOperations).add(eq(MobilityDriverLocationCacheService.GEO_KEY),
                eq(new Point(78.3772, 17.4435)), eq("88"));

        // Dirty location snapshot should contain driver 88
        Map<Long, CachedDriverLocation> dirty = cacheService.drainDirtyLocations();
        assertThat(dirty).containsKey(88L);
        assertThat(dirty.get(88L).lat()).isEqualTo(17.4435);
        assertThat(cacheService.drainDirtyLocations()).isEmpty();

        // Read back location
        when(hashOperations.entries("mobility:driver:88:location")).thenReturn(Map.of(
                "lat", "17.4435",
                "lng", "78.3772",
                "updatedAt", "1700000000000"
        ));
        Optional<CachedDriverLocation> loc = cacheService.getLocation(88L);
        assertThat(loc).isPresent();
        assertThat(loc.get().lng()).isEqualTo(78.3772);

        // GEO nearest query
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = new GeoResults<>(List.of(
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>("88", new Point(78.3772, 17.4435)), new Distance(0.5))
        ));
        when(geoOperations.radius(eq(MobilityDriverLocationCacheService.GEO_KEY), any(Circle.class), any()))
                .thenReturn(geoResults);
        List<Long> nearest = cacheService.findNearestDriverIds(17.4400, 78.3750, 25.0, 10);
        assertThat(nearest).containsExactly(88L);

        // Null-Redis fallback returns false / empty
        MobilityDriverLocationCacheService noRedis = new MobilityDriverLocationCacheService(null);
        assertThat(noRedis.updateLocation(88L, 17.44, 78.37, true)).isFalse();
        assertThat(noRedis.getLocation(88L)).isEmpty();
        assertThat(noRedis.findNearestDriverIds(17.44, 78.37, 25.0, 10)).isEmpty();
    }
}
