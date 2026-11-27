package com.manacommunity.api.events.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.events.dto.EventAuctionItemResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Redis cache for active Event Auction item state ({@code event:auction:comm:{communityId}:item:{itemId}:latest-bid}).
 * Falls back directly to PostgreSQL when Redis is unavailable.
 */
@Slf4j
@Service
public class EventAuctionCacheService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public EventAuctionCacheService(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public EventAuctionItemResponse getItem(
            Long communityId, Long itemId, Supplier<EventAuctionItemResponse> dbLoader) {
        if (redisTemplate == null || communityId == null || itemId == null) {
            return dbLoader.get();
        }
        String key = buildKey(communityId, itemId);
        try {
            String cachedJson = redisTemplate.opsForValue().get(key);
            if (cachedJson != null && !cachedJson.isBlank()) {
                log.debug("Cache HIT for event auction item key={}", key);
                return objectMapper.readValue(cachedJson, EventAuctionItemResponse.class);
            }
        } catch (Exception e) {
            log.warn("Redis read error for event auction key {}: {}", key, e.getMessage());
        }

        EventAuctionItemResponse loaded = dbLoader.get();
        setItem(communityId, itemId, loaded);
        return loaded;
    }

    public void setItem(Long communityId, Long itemId, EventAuctionItemResponse item) {
        if (redisTemplate == null || communityId == null || itemId == null || item == null) return;
        String key = buildKey(communityId, itemId);
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(item), CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache event auction item key {}: {}", key, e.getMessage());
        }
    }

    public void invalidate(Long communityId, Long itemId) {
        if (redisTemplate == null || communityId == null || itemId == null) return;
        try {
            redisTemplate.delete(buildKey(communityId, itemId));
        } catch (Exception ignored) {}
    }

    private static String buildKey(Long communityId, Long itemId) {
        return "event:auction:comm:" + communityId + ":item:" + itemId + ":latest-bid";
    }
}
