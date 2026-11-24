package com.manacommunity.api.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * JWT token blacklist — backed by Redis when available, in-memory otherwise.
 *
 * <p>When a user logs out, their token's unique {@code jti} (JWT ID) claim is recorded
 * here together with its expiry timestamp. The {@link JwtAuthenticationFilter} checks
 * this store on every request so that a discarded token cannot be re-used within its
 * remaining lifetime.</p>
 *
 * <p>Implementation notes:
 * <ul>
 *   <li>When Redis is available: uses a Redis key {@code token:blacklist:{jti}} with a
 *       TTL equal to the remaining token lifetime. This works correctly across all nodes
 *       in a multi-instance deployment.</li>
 *   <li>When Redis is unavailable: falls back to an in-memory {@link ConcurrentHashMap}
 *       so the service continues to operate on a single node.</li>
 *   <li>Call-sites ({@code blacklist()}, {@code isBlacklisted()}) are unchanged.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    /** Fallback in-memory store: jti → absolute expiry epoch-milliseconds */
    private final ConcurrentHashMap<String, Long> localBlacklist = new ConcurrentHashMap<>();

    public TokenBlacklistService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        if (redisTemplate == null) {
            log.warn("TokenBlacklistService: Redis not available — using in-memory blacklist (single-node only).");
        } else {
            log.info("TokenBlacklistService: using Redis-backed distributed blacklist.");
        }
    }

    /**
     * Records a token's {@code jti} as invalidated until {@code expiryEpochMs}.
     *
     * @param jti           the unique JWT ID claim
     * @param expiryEpochMs the token's absolute expiry in epoch-milliseconds
     */
    public void blacklist(String jti, long expiryEpochMs) {
        if (jti == null || jti.isBlank()) return;
        long ttlMs = expiryEpochMs - System.currentTimeMillis();
        if (ttlMs <= 0) return; // already expired — nothing to blacklist

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", ttlMs, TimeUnit.MILLISECONDS);
                log.debug("Token jti={} blacklisted in Redis for {}ms", jti, ttlMs);
                return;
            } catch (Exception e) {
                log.warn("Redis blacklist write failed for jti={}, falling back to in-memory: {}", jti, e.getMessage());
            }
        }

        // Fallback
        localBlacklist.put(jti, expiryEpochMs);
        log.debug("Token jti={} blacklisted in memory until epoch={}", jti, expiryEpochMs);
    }

    /**
     * Returns {@code true} when the given {@code jti} has been explicitly invalidated
     * AND its entry has not yet expired.
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) return false;

        if (redisTemplate != null) {
            try {
                return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
            } catch (Exception e) {
                log.warn("Redis blacklist check failed for jti={}, falling back to in-memory: {}", jti, e.getMessage());
            }
        }

        // Fallback
        Long expiry = localBlacklist.get(jti);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            localBlacklist.remove(jti);
            return false;
        }
        return true;
    }

    /**
     * Scheduled cleanup — removes expired in-memory fallback entries.
     * Redis entries self-expire via TTL; this only cleans the fallback map.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300_000)
    public void evictExpiredEntries() {
        if (localBlacklist.isEmpty()) return;
        long now = System.currentTimeMillis();
        int before = localBlacklist.size();
        localBlacklist.entrySet().removeIf(e -> now > e.getValue());
        int removed = before - localBlacklist.size();
        if (removed > 0) {
            log.debug("TokenBlacklist (in-memory fallback): evicted {} expired entries, {} remaining", removed, localBlacklist.size());
        }
    }
}
