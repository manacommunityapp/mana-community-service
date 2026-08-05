package com.manacommunity.api.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory JWT token blacklist.
 *
 * <p>When a user logs out, their token's unique {@code jti} (JWT ID) claim is recorded
 * here together with its expiry timestamp. The {@link JwtAuthenticationFilter} checks
 * this store on every request so that a discarded token cannot be re-used within its
 * remaining lifetime.</p>
 *
 * <p>Implementation notes:
 * <ul>
 *   <li>Backed by a {@link ConcurrentHashMap} — no external dependency required for
 *       single-instance Lightsail deployments.</li>
 *   <li>A scheduled job evicts expired entries every 5 minutes so the map never grows
 *       unbounded (entries are naturally self-expiring).</li>
 *   <li>If the service is ever scaled to multiple instances, swap the backing store for
 *       a Redis {@code SET} with the same TTL and the call-sites stay unchanged.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
public class TokenBlacklistService {

    /** jti → absolute expiry epoch-milliseconds */
    private final ConcurrentHashMap<String, Long> blacklist = new ConcurrentHashMap<>();

    /**
     * Records a token's {@code jti} as invalidated until {@code expiryEpochMs}.
     *
     * @param jti        the unique JWT ID claim
     * @param expiryEpochMs  the token's absolute expiry in epoch-milliseconds
     */
    public void blacklist(String jti, long expiryEpochMs) {
        if (jti == null || jti.isBlank()) return;
        blacklist.put(jti, expiryEpochMs);
        log.debug("Token jti={} blacklisted until epoch={}", jti, expiryEpochMs);
    }

    /**
     * Returns {@code true} when the given {@code jti} has been explicitly invalidated
     * AND its entry has not yet expired.
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) return false;
        Long expiry = blacklist.get(jti);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            blacklist.remove(jti); // lazy eviction on hit
            return false;
        }
        return true;
    }

    /**
     * Scheduled cleanup — removes all entries whose token has already expired.
     * Runs every 5 minutes to prevent unbounded memory growth.
     */
    @Scheduled(fixedRate = 300_000)
    public void evictExpiredEntries() {
        long now = System.currentTimeMillis();
        int before = blacklist.size();
        blacklist.entrySet().removeIf(e -> now > e.getValue());
        int removed = before - blacklist.size();
        if (removed > 0) {
            log.debug("TokenBlacklist: evicted {} expired entries, {} remaining", removed, blacklist.size());
        }
    }
}
