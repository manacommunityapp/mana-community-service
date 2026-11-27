package com.manacommunity.api.sports.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.sports.dto.SportsAnalyticsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.function.Supplier;

/**
 * Redis cache for {@link SportsAnalyticsResponse} aggregations.
 *
 * <p>Uses a versioned key prefix ({@code analytics:v{ver}:...}) so any match or
 * tournament completion can invalidate all cached filter combinations in O(1) via
 * {@code INCR analytics:version}, without scanning keys.</p>
 *
 * <p>Falls back directly to the DB loader when Redis is unavailable.</p>
 */
@Slf4j
@Service
public class SportsAnalyticsCacheService {

    private static final String VERSION_KEY = "analytics:version";
    private static final String KEY_PREFIX  = "analytics:v";
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SportsAnalyticsCacheService(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public SportsAnalyticsResponse getAnalytics(
            Long communityId, Long sportId, LocalDate fromDate, LocalDate toDate,
            Supplier<SportsAnalyticsResponse> dbLoader) {

        if (redisTemplate == null) {
            return dbLoader.get();
        }

        String cacheKey = buildKey(currentVersion(), communityId, sportId, fromDate, toDate);
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null && !cachedJson.isBlank()) {
                log.debug("Cache HIT for sports analytics key={}", cacheKey);
                return objectMapper.readValue(cachedJson, SportsAnalyticsResponse.class);
            }
        } catch (Exception e) {
            log.warn("Redis read error for analytics key {}: {}", cacheKey, e.getMessage());
        }

        log.debug("Cache MISS for sports analytics key={}, loading from DB", cacheKey);
        SportsAnalyticsResponse response = dbLoader.get();
        if (response != null) {
            try {
                String json = objectMapper.writeValueAsString(response);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
            } catch (Exception e) {
                log.warn("Failed to cache sports analytics in Redis for key {}: {}", cacheKey, e.getMessage());
            }
        }
        return response;
    }

    /**
     * Atomically invalidates all cached analytics snapshots in O(1) by advancing the version counter.
     */
    public void invalidateAll() {
        if (redisTemplate == null) return;
        try {
            redisTemplate.opsForValue().increment(VERSION_KEY);
            log.debug("Invalidated sports analytics cache via version increment");
        } catch (Exception e) {
            log.warn("Failed to invalidate sports analytics cache: {}", e.getMessage());
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

    private static String buildKey(long version, Long communityId, Long sportId, LocalDate fromDate, LocalDate toDate) {
        return KEY_PREFIX + version
                + ":community:" + (communityId != null ? communityId : "all")
                + ":sport:" + (sportId != null ? sportId : "all")
                + ":" + (fromDate != null ? fromDate : "any")
                + ":" + (toDate != null ? toDate : "any");
    }
}
