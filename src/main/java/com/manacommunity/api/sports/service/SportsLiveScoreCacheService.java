package com.manacommunity.api.sports.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.sports.dto.SportsGenericMatchStateResponse;
import com.manacommunity.api.sports.dto.SportsLiveMatchStateResponse;
import com.manacommunity.api.sports.repository.SportsMatchEventOutboxRepository;
import com.manacommunity.api.sports.scheduler.SportsMatchEventOutbox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Slf4j
@Service
public class SportsLiveScoreCacheService {

    private static final String KEY_PREFIX = "match:";
    private static final String LIVE_SUFFIX = ":live";
    private static final String SEQ_SUFFIX = ":seq";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final SportsMatchEventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    // In-memory fallback counters when Redis is unavailable
    private final ConcurrentHashMap<Long, AtomicLong> localSequenceCounters = new ConcurrentHashMap<>();

    public SportsLiveScoreCacheService(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            SportsMatchEventOutboxRepository outboxRepository,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    private static String tenantPrefix() {
        Long communityId = com.manacommunity.api.config.tenant.TenantContext.getCommunityId();
        return communityId != null ? ("comm:" + communityId + ":") : "";
    }

    private static String buildLiveKey(Long matchId) {
        return tenantPrefix() + KEY_PREFIX + matchId + LIVE_SUFFIX;
    }

    private static String buildGenericLiveKey(Long matchId) {
        return tenantPrefix() + KEY_PREFIX + "generic:" + matchId + LIVE_SUFFIX;
    }

    private static String buildSeqKey(Long matchId) {
        return tenantPrefix() + KEY_PREFIX + matchId + SEQ_SUFFIX;
    }

    public SportsLiveMatchStateResponse getLiveSnapshot(Long matchId, Supplier<SportsLiveMatchStateResponse> dbLoader) {
        if (matchId == null) return null;
        String cacheKey = buildLiveKey(matchId);

        if (redisTemplate != null) {
            try {
                String cachedJson = redisTemplate.opsForValue().get(cacheKey);
                if (cachedJson != null && !cachedJson.isBlank()) {
                    log.debug("Cache HIT for live match snapshot matchId={}", matchId);
                    return objectMapper.readValue(cachedJson, SportsLiveMatchStateResponse.class);
                }
            } catch (Exception e) {
                log.warn("Redis read error for key {}: {}", cacheKey, e.getMessage());
            }
        }

        log.debug("Cache MISS for live match snapshot matchId={}, loading from DB", matchId);
        SportsLiveMatchStateResponse snapshot = dbLoader.get();
        if (snapshot != null) {
            setLiveSnapshot(matchId, snapshot);
        }
        return snapshot;
    }

    public void setLiveSnapshot(Long matchId, SportsLiveMatchStateResponse snapshot) {
        if (matchId == null || snapshot == null) return;
        String cacheKey = buildLiveKey(matchId);

        if (redisTemplate != null) {
            try {
                String json = objectMapper.writeValueAsString(snapshot);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
                log.debug("Updated Redis snapshot for matchId={}", matchId);
            } catch (Exception e) {
                log.warn("Failed to cache live snapshot in Redis for matchId={}: {}", matchId, e.getMessage());
            }
        }
    }

    public long getNextSequence(Long matchId) {
        if (matchId == null) return 1L;
        String seqKey = buildSeqKey(matchId);

        if (redisTemplate != null) {
            try {
                Boolean exists = redisTemplate.hasKey(seqKey);
                if (Boolean.FALSE.equals(exists)) {
                    Long maxSeq = outboxRepository.findMaxSequenceNoByMatchId(matchId);
                    long initial = maxSeq != null ? maxSeq : 0L;
                    redisTemplate.opsForValue().set(seqKey, String.valueOf(initial), CACHE_TTL);
                }
                Long next = redisTemplate.opsForValue().increment(seqKey);
                return next != null ? next : 1L;
            } catch (Exception e) {
                log.warn("Redis INCR failed for seq key {}: {}", seqKey, e.getMessage());
            }
        }

        // In-memory fallback
        return localSequenceCounters.computeIfAbsent(matchId, id -> {
            Long maxSeq = outboxRepository.findMaxSequenceNoByMatchId(id);
            return new AtomicLong(maxSeq != null ? maxSeq : 0L);
        }).incrementAndGet();
    }

    @Transactional
    public SportsMatchEventOutbox recordOutboxEvent(Long matchId, Long sequence, String eventType, Object payload) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            payloadJson = "{}";
        }

        SportsMatchEventOutbox outbox = SportsMatchEventOutbox.builder()
                .matchId(matchId)
                .sequenceNo(sequence)
                .eventType(eventType)
                .payload(payloadJson)
                .status("PUBLISHED")
                .publishedAt(LocalDateTime.now())
                .build();

        return outboxRepository.save(outbox);
    }

    public void invalidate(Long matchId) {
        if (matchId == null) return;
        String cacheKey = buildLiveKey(matchId);
        String genericCacheKey = buildGenericLiveKey(matchId);
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(cacheKey);
                redisTemplate.delete(genericCacheKey);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Cleanup invoked when a match transitions to {@code COMPLETED}.
     *
     * <p>Deletes live score snapshots, enforces a bounded 24h TTL on the sequence counter
     * key so completed match keys never accumulate indefinitely in Redis, and clears the
     * in-memory fallback sequence counter for {@code matchId}.</p>
     */
    public void cleanupOnMatchCompleted(Long matchId) {
        if (matchId == null) return;
        invalidate(matchId);
        localSequenceCounters.remove(matchId);

        if (redisTemplate != null) {
            try {
                String seqKey = buildSeqKey(matchId);
                if (Boolean.TRUE.equals(redisTemplate.hasKey(seqKey))) {
                    redisTemplate.expire(seqKey, CACHE_TTL);
                }
                log.debug("Cleaned up and applied 24h TTL on completed match keys matchId={}", matchId);
            } catch (Exception e) {
                log.warn("Failed TTL cleanup on completed matchId={}: {}", matchId, e.getMessage());
            }
        }
    }

    public SportsGenericMatchStateResponse getGenericLiveSnapshot(Long matchId, Supplier<SportsGenericMatchStateResponse> dbLoader) {
        if (matchId == null) return null;
        String cacheKey = buildGenericLiveKey(matchId);

        if (redisTemplate != null) {
            try {
                String cachedJson = redisTemplate.opsForValue().get(cacheKey);
                if (cachedJson != null && !cachedJson.isBlank()) {
                    log.debug("Cache HIT for generic live match snapshot matchId={}", matchId);
                    return objectMapper.readValue(cachedJson, SportsGenericMatchStateResponse.class);
                }
            } catch (Exception e) {
                log.warn("Redis read error for key {}: {}", cacheKey, e.getMessage());
            }
        }

        log.debug("Cache MISS for generic live match snapshot matchId={}, loading from DB", matchId);
        SportsGenericMatchStateResponse snapshot = dbLoader.get();
        if (snapshot != null) {
            setGenericLiveSnapshot(matchId, snapshot);
        }
        return snapshot;
    }

    public void setGenericLiveSnapshot(Long matchId, SportsGenericMatchStateResponse snapshot) {
        if (matchId == null || snapshot == null) return;
        String cacheKey = buildGenericLiveKey(matchId);

        if (redisTemplate != null) {
            try {
                String json = objectMapper.writeValueAsString(snapshot);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
                log.debug("Updated Redis snapshot for generic matchId={}", matchId);
            } catch (Exception e) {
                log.warn("Failed to cache generic live snapshot in Redis for matchId={}: {}", matchId, e.getMessage());
            }
        }
    }

    public void invalidateGeneric(Long matchId) {
        if (matchId == null) return;
        String cacheKey = buildGenericLiveKey(matchId);
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(cacheKey);
            } catch (Exception ignored) {}
        }
    }
}
