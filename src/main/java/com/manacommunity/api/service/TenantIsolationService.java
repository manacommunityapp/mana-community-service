package com.manacommunity.api.service;

import com.manacommunity.api.model.TenantResourceQuota;
import com.manacommunity.api.repository.TenantResourceQuotaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TenantIsolationService {

    private static final Logger log = LoggerFactory.getLogger(TenantIsolationService.class);

    private final TenantResourceQuotaRepository quotaRepo;
    private final ConcurrentHashMap<String, Semaphore> querySemaphores = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TokenBucket> rateLimiters = new ConcurrentHashMap<>();

    public TenantIsolationService(TenantResourceQuotaRepository quotaRepo) {
        this.quotaRepo = quotaRepo;
    }

    public boolean acquireQuerySlot(Long communityId, String resourceType, long timeoutMs) {
        String key = communityId + ":" + resourceType;
        TenantResourceQuota quota = getOrCreateQuota(communityId, resourceType);
        Semaphore semaphore = querySemaphores.computeIfAbsent(key,
                k -> new Semaphore(quota.getMaxConcurrentQueries()));

        try {
            boolean acquired = semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
            if (!acquired) {
                log.warn("Community {} exceeded concurrent query limit for {}", communityId, resourceType);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void releaseQuerySlot(Long communityId, String resourceType) {
        String key = communityId + ":" + resourceType;
        Semaphore semaphore = querySemaphores.get(key);
        if (semaphore != null) {
            semaphore.release();
        }
    }

    public boolean checkRateLimit(Long communityId, String resourceType) {
        String key = communityId + ":" + resourceType;
        TenantResourceQuota quota = getOrCreateQuota(communityId, resourceType);
        TokenBucket bucket = rateLimiters.computeIfAbsent(key,
                k -> new TokenBucket(quota.getRateLimitPerMinute(), quota.getBurstTokens()));
        return bucket.tryConsume();
    }

    public int getExportLimit(Long communityId) {
        TenantResourceQuota quota = getOrCreateQuota(communityId, "EXPORT");
        return quota.getMaxRowsPerExport();
    }

    public int getPriorityWeight(Long communityId) {
        TenantResourceQuota quota = getOrCreateQuota(communityId, "GENERAL");
        return quota.getPriorityWeight();
    }

    @Transactional
    public TenantResourceQuota updateQuota(Long communityId, String resourceType,
                                            Integer maxConcurrent, Integer maxRows,
                                            Integer rateLimit, Integer priority) {
        TenantResourceQuota quota = getOrCreateQuota(communityId, resourceType);
        if (maxConcurrent != null) quota.setMaxConcurrentQueries(maxConcurrent);
        if (maxRows != null) quota.setMaxRowsPerExport(maxRows);
        if (rateLimit != null) quota.setRateLimitPerMinute(rateLimit);
        if (priority != null) quota.setPriorityWeight(priority);

        String key = communityId + ":" + resourceType;
        querySemaphores.remove(key);
        rateLimiters.remove(key);

        return quotaRepo.save(quota);
    }

    @Transactional(readOnly = true)
    public List<TenantResourceQuota> getQuotas(Long communityId) {
        return quotaRepo.findByCommunityIdAndActiveTrue(communityId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTenantMetrics(Long communityId) {
        List<TenantResourceQuota> quotas = quotaRepo.findByCommunityIdAndActiveTrue(communityId);
        return Map.of(
                "communityId", communityId,
                "quotas", quotas.stream().map(q -> Map.of(
                        "resourceType", q.getResourceType(),
                        "maxConcurrentQueries", q.getMaxConcurrentQueries(),
                        "maxRowsPerExport", q.getMaxRowsPerExport(),
                        "rateLimitPerMinute", q.getRateLimitPerMinute(),
                        "priorityWeight", q.getPriorityWeight()
                )).toList(),
                "activeConnections", getActiveConnections(communityId)
        );
    }

    private int getActiveConnections(Long communityId) {
        return querySemaphores.entrySet().stream()
                .filter(e -> e.getKey().startsWith(communityId + ":"))
                .mapToInt(e -> e.getValue().getQueueLength())
                .sum();
    }

    private TenantResourceQuota getOrCreateQuota(Long communityId, String resourceType) {
        Optional<TenantResourceQuota> existing = quotaRepo.findByCommunityIdAndResourceType(communityId, resourceType);
        return existing.orElseGet(() -> {
            TenantResourceQuota quota = TenantResourceQuota.builder()
                    .community(null)
                    .resourceType(resourceType)
                    .build();
            quota.setMaxConcurrentQueries(5);
            quota.setMaxRowsPerExport(50000);
            quota.setRateLimitPerMinute(60);
            quota.setPriorityWeight(10);
            return quota;
        });
    }

    private static class TokenBucket {
        private final int maxTokens;
        private final int refillRate;
        private final AtomicLong tokens;
        private volatile long lastRefill;

        TokenBucket(int refillPerMinute, int burst) {
            this.maxTokens = burst;
            this.refillRate = refillPerMinute;
            this.tokens = new AtomicLong(burst);
            this.lastRefill = System.currentTimeMillis();
        }

        boolean tryConsume() {
            refill();
            long current = tokens.get();
            if (current <= 0) return false;
            return tokens.compareAndSet(current, current - 1);
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefill;
            if (elapsed > 1000) {
                long newTokens = (elapsed * refillRate) / 60000;
                if (newTokens > 0) {
                    tokens.updateAndGet(t -> Math.min(t + newTokens, maxTokens));
                    lastRefill = now;
                }
            }
        }
    }
}
