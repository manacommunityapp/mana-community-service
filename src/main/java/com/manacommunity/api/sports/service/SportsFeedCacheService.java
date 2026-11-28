package com.manacommunity.api.sports.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.sports.dto.SportsFeedMatchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Redis cache for the community sports "Live Now" feed strip.
 *
 * <p>Uses a versioned key ({@code sports:feed:live-strip:v{ver}:community:{id}}) with a 60s TTL
 * and O(1) invalidation via {@code INCR sports:feed:version} whenever
 * {@link SportsFeedService#broadcastLiveFeedUpdate()} triggers.</p>
 */
@Slf4j
@Service
public class SportsFeedCacheService {

    private static final String VERSION_KEY    = "sports:feed:version";
    private static final String KEY_PREFIX     = "sports:feed:live-strip:v";
    private static final String UPCOMING_PREFIX = "sports:feed:upcoming:v";
    private static final Duration LIVE_TTL     = Duration.ofSeconds(60);
    private static final Duration UPCOMING_TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SportsFeedCacheService(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<SportsFeedMatchResponse> getLiveFeed(
            Long communityId,
            Supplier<List<SportsFeedMatchResponse>> dbLoader) {

        if (redisTemplate == null) {
            return dbLoader.get();
        }

        String cacheKey = KEY_PREFIX + currentVersion() + ":community:" + (communityId != null ? communityId : "all");
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null && !cachedJson.isBlank()) {
                log.debug("Cache HIT for sports live-strip key={}", cacheKey);
                return objectMapper.readValue(cachedJson, new TypeReference<List<SportsFeedMatchResponse>>() {});
            }
        } catch (Exception e) {
            log.warn("Redis read error for live-strip key {}: {}", cacheKey, e.getMessage());
        }

        List<SportsFeedMatchResponse> matches = dbLoader.get();
        if (matches != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(matches), LIVE_TTL);
            } catch (Exception e) {
                log.warn("Failed to cache live-strip for key {}: {}", cacheKey, e.getMessage());
            }
        }
        return matches;
    }

    public List<SportsFeedMatchResponse> getUpcomingFeed(
            Long communityId,
            int limit,
            Supplier<List<SportsFeedMatchResponse>> dbLoader) {

        if (redisTemplate == null) {
            return dbLoader.get();
        }

        String cacheKey = UPCOMING_PREFIX + currentVersion() + ":community:"
                + (communityId != null ? communityId : "all") + ":limit:" + limit;
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null && !cachedJson.isBlank()) {
                log.debug("Cache HIT for sports upcoming-feed key={}", cacheKey);
                return objectMapper.readValue(cachedJson, new TypeReference<List<SportsFeedMatchResponse>>() {});
            }
        } catch (Exception e) {
            log.warn("Redis read error for upcoming-feed key {}: {}", cacheKey, e.getMessage());
        }

        List<SportsFeedMatchResponse> matches = dbLoader.get();
        if (matches != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(matches), UPCOMING_TTL);
            } catch (Exception e) {
                log.warn("Failed to cache upcoming-feed for key {}: {}", cacheKey, e.getMessage());
            }
        }
        return matches;
    }

    /**
     * Invalidates all cached live-strip entries across communities in O(1).
     */
    public void invalidateLiveFeed() {
        if (redisTemplate == null) return;
        try {
            redisTemplate.opsForValue().increment(VERSION_KEY);
            log.debug("Invalidated sports live-strip cache via version increment");
        } catch (Exception e) {
            log.warn("Failed to invalidate live-strip cache: {}", e.getMessage());
        }
    }

    private long currentVersion() {
        try {
            String v = redisTemplate.opsForValue().get(VERSION_KEY);
            return v != null ? Long.parseLong(v) : 1L;
        } catch (Exception e) {
            return 1L;
        }
    }
}
