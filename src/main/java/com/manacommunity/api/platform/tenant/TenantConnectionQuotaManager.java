package com.manacommunity.api.platform.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Fair-Share Tenant Connection Bulkhead Manager.
 *
 * <p>Prevents any single busy gated community from monopolizing the shared HikariCP connection pool
 * (directly resolving the LDT-002 pool exhaustion issue where 300 users locked up the pool).
 * Each tenant is assigned a maximum concurrent database connection lease quota.</p>
 */
@Component
@Slf4j
public class TenantConnectionQuotaManager {

    private final int defaultMaxLeasesPerTenant;
    private final long defaultAcquireTimeoutMs;

    // Per-tenant semaphore regulating concurrent active database connections
    private final ConcurrentHashMap<Long, Semaphore> tenantSemaphores = new ConcurrentHashMap<>();

    // Custom quota overrides per community (e.g. mega-township tier)
    private final ConcurrentHashMap<Long, Integer> quotaOverrides = new ConcurrentHashMap<>();

    public TenantConnectionQuotaManager(
            @Value("${mana.tenant.datasource.default-quota:12}") int defaultMaxLeasesPerTenant,
            @Value("${mana.tenant.datasource.acquire-timeout-ms:50}") long defaultAcquireTimeoutMs
    ) {
        this.defaultMaxLeasesPerTenant = defaultMaxLeasesPerTenant;
        this.defaultAcquireTimeoutMs = defaultAcquireTimeoutMs;
        log.info("[TenantBulkhead] Initialized with defaultQuota={} acquireTimeoutMs={}",
                defaultMaxLeasesPerTenant, defaultAcquireTimeoutMs);
    }

    /**
     * Set a custom connection lease quota for a high-volume mega-township.
     */
    public void setTenantQuota(Long communityId, int maxLeases) {
        quotaOverrides.put(communityId, maxLeases);
        tenantSemaphores.put(communityId, new Semaphore(maxLeases, true));
        log.info("[TenantBulkhead] Configured custom quota of {} connections for community {}", maxLeases, communityId);
    }

    /**
     * Acquire a connection lease for the given community with fail-fast timeout.
     *
     * @param communityId community ID (or null for platform system threads)
     * @return an AutoCloseable lease to be released upon transaction completion
     * @throws TenantConnectionQuotaExceededException if tenant exceeded their fair-share connection limit
     */
    public TenantLease acquireLease(Long communityId) throws TenantConnectionQuotaExceededException {
        return acquireLease(communityId, defaultAcquireTimeoutMs);
    }

    public TenantLease acquireLease(Long communityId, long timeoutMs) throws TenantConnectionQuotaExceededException {
        if (communityId == null || communityId <= 0L) {
            // System or non-tenant operational queries bypass bulkhead
            return () -> {};
        }

        int maxAllowed = quotaOverrides.getOrDefault(communityId, defaultMaxLeasesPerTenant);
        Semaphore semaphore = tenantSemaphores.computeIfAbsent(communityId, id -> new Semaphore(maxAllowed, true));

        boolean acquired;
        try {
            acquired = timeoutMs > 0
                    ? semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)
                    : semaphore.tryAcquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TenantConnectionQuotaExceededException(communityId, maxAllowed - semaphore.availablePermits(), maxAllowed);
        }

        if (!acquired) {
            int active = maxAllowed - semaphore.availablePermits();
            log.warn("[TenantBulkhead] Community {} reached connection quota: active={}/{} — blocking noisy neighbor",
                    communityId, active, maxAllowed);
            throw new TenantConnectionQuotaExceededException(communityId, active, maxAllowed);
        }

        return () -> {
            semaphore.release();
        };
    }

    public int getActiveLeases(Long communityId) {
        if (communityId == null) return 0;
        Semaphore semaphore = tenantSemaphores.get(communityId);
        if (semaphore == null) return 0;
        int maxAllowed = quotaOverrides.getOrDefault(communityId, defaultMaxLeasesPerTenant);
        return maxAllowed - semaphore.availablePermits();
    }

    public int getAvailableLeases(Long communityId) {
        if (communityId == null) return defaultMaxLeasesPerTenant;
        Semaphore semaphore = tenantSemaphores.get(communityId);
        if (semaphore == null) {
            return quotaOverrides.getOrDefault(communityId, defaultMaxLeasesPerTenant);
        }
        return semaphore.availablePermits();
    }

    public int getMaxAllowed(Long communityId) {
        if (communityId == null) return defaultMaxLeasesPerTenant;
        return quotaOverrides.getOrDefault(communityId, defaultMaxLeasesPerTenant);
    }

    public void reset() {
        tenantSemaphores.clear();
        quotaOverrides.clear();
    }

    @FunctionalInterface
    public interface TenantLease extends AutoCloseable {
        @Override
        void close();
    }
}
