package com.manacommunity.api.visitor.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prevents QR code replay attacks by tracking used JTIs in Redis.
 *
 * Each QR token carries a one-time JTI (JWT ID). On first scan:
 *   SET visitor:qr:jti:{jti} USED EX {remaining_ttl_seconds}
 *
 * On second scan the key exists → reject with "Already checked in."
 * TTL = token's remaining validity so Redis auto-evicts stale entries.
 *
 * Falls back to an in-process ConcurrentHashMap when Redis is unavailable
 * (dev/offline). The map is per-JVM so multi-replica replay protection
 * requires Redis.
 */
@Slf4j
@Service
public class QrReplayGuardService {

    private static final String KEY_PREFIX = "visitor:qr:jti:";
    private static final long MIN_TTL_SECONDS = 60L;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private final ConcurrentHashMap<String, Long> localFallback = new ConcurrentHashMap<>();

    /** Returns true if this JTI has already been used (replay detected). */
    public boolean isAlreadyUsed(String jti) {
        if (jti == null) return false;
        if (redisTemplate != null) {
            try {
                return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
            } catch (Exception ex) {
                log.warn("Redis unavailable for JTI check, using local fallback: {}", ex.getMessage());
            }
        }
        Long expiresAt = localFallback.get(jti);
        if (expiresAt == null) return false;
        if (Instant.now().getEpochSecond() > expiresAt) {
            localFallback.remove(jti);
            return false;
        }
        return true;
    }

    /**
     * Marks a JTI as used. Must be called BEFORE performing check-in to
     * close the race window between the hasKey check and the actual scan.
     *
     * @param jti          the token's unique ID
     * @param remainingTtl seconds until the token expires; clamped to MIN_TTL_SECONDS
     */
    public void markUsed(String jti, long remainingTtl) {
        if (jti == null) return;
        long ttl = Math.max(remainingTtl, MIN_TTL_SECONDS);
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(KEY_PREFIX + jti, "USED", Duration.ofSeconds(ttl));
                return;
            } catch (Exception ex) {
                log.warn("Redis unavailable for JTI mark, using local fallback: {}", ex.getMessage());
            }
        }
        localFallback.put(jti, Instant.now().getEpochSecond() + ttl);
    }
}
