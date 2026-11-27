package com.manacommunity.api.sports.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.sports.dto.SportsDashboardResponse.Stats;
import com.manacommunity.api.sports.dto.SportsDashboardResponse.UpcomingEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Redis cache for {@link SportsDashboardService} read endpoints ({@code upcoming-events} and {@code stats}).
 *
 * <p>Uses a versioned key prefix ({@code dashboard:v{ver}:...}) for O(1) cluster-wide
 * invalidation without key scanning. Falls back cleanly to DB when Redis is unavailable.</p>
 */
@Slf4j
@Service
public class SportsDashboardCacheService {

    private static final String VERSION_KEY        = "dashboard:version";
    private static final String KEY_PREFIX         = "dashboard:v";
    private static final Duration UPCOMING_TTL     = Duration.ofMinutes(5);
    private static final Duration STATS_TTL        = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SportsDashboardCacheService(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<UpcomingEvent> getUpcomingEvents(Long userId, Supplier<List<UpcomingEvent>> dbLoader) {
        if (redisTemplate == null || userId == null) {
            return dbLoader.get();
        }
        String cacheKey = KEY_PREFIX + currentVersion() + ":user:" + userId + ":upcoming-events";
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null && !cachedJson.isBlank()) {
                log.debug("Cache HIT for dashboard upcoming-events userId={}", userId);
                return objectMapper.readValue(cachedJson, new TypeReference<List<UpcomingEvent>>() {});
            }
        } catch (Exception e) {
            log.warn("Redis read error for dashboard key {}: {}", cacheKey, e.getMessage());
        }

        List<UpcomingEvent> result = dbLoader.get();
        if (result != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), UPCOMING_TTL);
            } catch (Exception e) {
                log.warn("Failed to cache dashboard upcoming-events for userId={}: {}", userId, e.getMessage());
            }
        }
        return result;
    }

    public Stats getStats(Long userId, boolean isSuperAdmin, Long communityId, Supplier<Stats> dbLoader) {
        if (redisTemplate == null || userId == null) {
            return dbLoader.get();
        }
        String scope = isSuperAdmin ? "super" : ("comm:" + (communityId != null ? communityId : "none"));
        String cacheKey = KEY_PREFIX + currentVersion() + ":stats:user:" + userId + ":" + scope;
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null && !cachedJson.isBlank()) {
                log.debug("Cache HIT for dashboard stats userId={}", userId);
                return objectMapper.readValue(cachedJson, Stats.class);
            }
        } catch (Exception e) {
            log.warn("Redis read error for dashboard stats key {}: {}", cacheKey, e.getMessage());
        }

        Stats result = dbLoader.get();
        if (result != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), STATS_TTL);
            } catch (Exception e) {
                log.warn("Failed to cache dashboard stats for userId={}: {}", userId, e.getMessage());
            }
        }
        return result;
    }

    public void invalidateAll() {
        if (redisTemplate == null) return;
        try {
            redisTemplate.opsForValue().increment(VERSION_KEY);
            log.debug("Invalidated sports dashboard cache via version increment");
        } catch (Exception e) {
            log.warn("Failed to invalidate sports dashboard cache: {}", e.getMessage());
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
