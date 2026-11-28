package com.manacommunity.api.platform.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-tenant query execution timeout registry for noisy-neighbor protection.
 *
 * <p>Enforces bounded PostgreSQL {@code statement_timeout} values per community so
 * expensive analytics, AGM voting reports, or unindexed queries from one society
 * abort cleanly instead of starving shared connections for peer communities.</p>
 */
@Service
@Slf4j
public class TenantQueryTimeoutService {

    private static final long MIN_TIMEOUT_MS = 500L;
    private static final long MAX_TIMEOUT_MS = 30_000L;

    private final long defaultTimeoutMs;
    private final ConcurrentHashMap<Long, Long> tenantTimeoutsMs = new ConcurrentHashMap<>();

    public TenantQueryTimeoutService(
            @Value("${mana.tenant.query.default-statement-timeout-ms:5000}") long defaultTimeoutMs
    ) {
        this.defaultTimeoutMs = clamp(defaultTimeoutMs);
        log.info("[TenantQueryTimeout] Initialized default statement_timeout={}ms", this.defaultTimeoutMs);
    }

    public void setTenantStatementTimeout(Long communityId, long timeoutMs) {
        if (communityId == null || communityId <= 0L) {
            return;
        }
        long clamped = clamp(timeoutMs);
        tenantTimeoutsMs.put(communityId, clamped);
        log.info("[TenantQueryTimeout] Set custom statement_timeout={}ms for community {}", clamped, communityId);
    }

    public long getStatementTimeoutMs(Long communityId) {
        if (communityId == null || communityId <= 0L) {
            return defaultTimeoutMs;
        }
        return tenantTimeoutsMs.getOrDefault(communityId, defaultTimeoutMs);
    }

    public void reset() {
        tenantTimeoutsMs.clear();
    }

    private long clamp(long ms) {
        return Math.max(MIN_TIMEOUT_MS, Math.min(MAX_TIMEOUT_MS, ms));
    }
}
