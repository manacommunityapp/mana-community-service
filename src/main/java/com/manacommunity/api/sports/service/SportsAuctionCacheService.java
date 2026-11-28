package com.manacommunity.api.sports.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.sports.dto.PlayerWithBidResponse;
import com.manacommunity.api.sports.dto.SportsAuctionStatsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Service
public class SportsAuctionCacheService {

    private static final String KEY_PREFIX = "auction:";
    private static final String CURRENT_PLAYER_SUFFIX = ":current-player";
    private static final String STATS_SUFFIX = ":stats";
    private static final Duration CACHE_TTL = Duration.ofHours(2);
    private static final Duration STATS_TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SportsAuctionCacheService(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public PlayerWithBidResponse getCurrentPlayer(Long configId, Supplier<PlayerWithBidResponse> dbLoader) {
        if (configId == null) return null;
        String cacheKey = KEY_PREFIX + configId + CURRENT_PLAYER_SUFFIX;

        if (redisTemplate != null) {
            try {
                String cachedJson = redisTemplate.opsForValue().get(cacheKey);
                if (cachedJson != null && !cachedJson.isBlank()) {
                    log.debug("Cache HIT for current auction player configId={}", configId);
                    return objectMapper.readValue(cachedJson, PlayerWithBidResponse.class);
                }
            } catch (Exception e) {
                log.warn("Redis read error for key {}: {}", cacheKey, e.getMessage());
            }
        }

        log.debug("Cache MISS for current auction player configId={}, loading from DB", configId);
        PlayerWithBidResponse snapshot = dbLoader.get();
        if (snapshot != null) {
            setCurrentPlayer(configId, snapshot);
        }
        return snapshot;
    }

    public void setCurrentPlayer(Long configId, PlayerWithBidResponse player) {
        if (configId == null || player == null) return;
        String cacheKey = KEY_PREFIX + configId + CURRENT_PLAYER_SUFFIX;

        if (redisTemplate != null) {
            try {
                String json = objectMapper.writeValueAsString(player);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
                log.debug("Updated Redis current player for auction configId={}", configId);
            } catch (Exception e) {
                log.warn("Failed to cache current player in Redis for configId={}: {}", configId, e.getMessage());
            }
        }
    }

    public SportsAuctionStatsResponse getAuctionStats(Long configId, Supplier<SportsAuctionStatsResponse> dbLoader) {
        if (configId == null) return null;
        String cacheKey = KEY_PREFIX + configId + STATS_SUFFIX;

        if (redisTemplate != null) {
            try {
                String cachedJson = redisTemplate.opsForValue().get(cacheKey);
                if (cachedJson != null && !cachedJson.isBlank()) {
                    log.debug("Cache HIT for auction stats configId={}", configId);
                    return objectMapper.readValue(cachedJson, SportsAuctionStatsResponse.class);
                }
            } catch (Exception e) {
                log.warn("Redis read error for stats key {}: {}", cacheKey, e.getMessage());
            }
        }

        log.debug("Cache MISS for auction stats configId={}, loading from DB", configId);
        SportsAuctionStatsResponse stats = dbLoader.get();
        if (stats != null) {
            setAuctionStats(configId, stats);
        }
        return stats;
    }

    public void setAuctionStats(Long configId, SportsAuctionStatsResponse stats) {
        if (configId == null || stats == null) return;
        String cacheKey = KEY_PREFIX + configId + STATS_SUFFIX;

        if (redisTemplate != null) {
            try {
                String json = objectMapper.writeValueAsString(stats);
                redisTemplate.opsForValue().set(cacheKey, json, STATS_TTL);
                log.debug("Cached auction stats in Redis for configId={} (TTL={}s)", configId, STATS_TTL.toSeconds());
            } catch (Exception e) {
                log.warn("Failed to cache auction stats in Redis for configId={}: {}", configId, e.getMessage());
            }
        }
    }

    public void invalidateStats(Long configId) {
        if (configId == null || redisTemplate == null) return;
        try {
            redisTemplate.delete(KEY_PREFIX + configId + STATS_SUFFIX);
        } catch (Exception ignored) {}
    }

    public void invalidate(Long configId) {
        if (configId == null) return;
        String playerKey = KEY_PREFIX + configId + CURRENT_PLAYER_SUFFIX;
        String statsKey = KEY_PREFIX + configId + STATS_SUFFIX;
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(playerKey);
                redisTemplate.delete(statsKey);
                log.debug("Invalidated auction cache (current-player & stats) for configId={}", configId);
            } catch (Exception ignored) {}
        }
    }
}
