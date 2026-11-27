package com.manacommunity.api.iot.edge.service;

import com.manacommunity.api.iot.edge.entity.BarrierValidationCache;
import com.manacommunity.api.iot.edge.repository.BarrierValidationCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BarrierAccessService {

    private final BarrierValidationCacheRepository cacheRepository;

    private static final Duration CLOUD_TIMEOUT = Duration.ofMillis(200);
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final Duration VALIDATION_BUDGET = Duration.ofMillis(300);

    private final ConcurrentHashMap<String, BarrierDecision> localHotCache = new ConcurrentHashMap<>();
    private final ExecutorService validationExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());

    public BarrierDecision validateAccess(Long communityId, String credentialType, String credentialValue) {
        long startNanos = System.nanoTime();
        String cacheKey = communityId + ":" + credentialType + ":" + credentialValue;

        BarrierDecision hotCacheHit = localHotCache.get(cacheKey);
        if (hotCacheHit != null && hotCacheHit.validUntil().isAfter(Instant.now())) {
            long elapsed = (System.nanoTime() - startNanos) / 1_000_000;
            log.debug("Hot cache hit for {} in {}ms", cacheKey, elapsed);
            return hotCacheHit.withLatency(elapsed);
        }

        Future<BarrierDecision> cloudFuture = validationExecutor.submit(
                () -> performCloudValidation(communityId, credentialType, credentialValue));

        try {
            BarrierDecision cloudResult = cloudFuture.get(CLOUD_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            localHotCache.put(cacheKey, cloudResult);
            updateDbCache(communityId, credentialType, credentialValue, cloudResult);

            long elapsed = (System.nanoTime() - startNanos) / 1_000_000;
            log.debug("Cloud validation for {} in {}ms", cacheKey, elapsed);
            return cloudResult.withLatency(elapsed);

        } catch (TimeoutException e) {
            cloudFuture.cancel(true);
            log.warn("Cloud validation timeout for {}, falling back to cache", cacheKey);
            return fallbackToCache(communityId, credentialType, credentialValue, startNanos);

        } catch (Exception e) {
            log.warn("Cloud validation failed for {}: {}, falling back to cache", cacheKey, e.getMessage());
            return fallbackToCache(communityId, credentialType, credentialValue, startNanos);
        }
    }

    private BarrierDecision performCloudValidation(Long communityId, String credentialType, String credentialValue) {
        // In production this calls the resident/vehicle authorization microservice.
        // The logic here represents the validation contract.
        boolean authorized = true; // placeholder — real impl queries resident DB
        String residentName = "Resident"; // placeholder
        String flatNo = ""; // placeholder

        if (authorized) {
            return new BarrierDecision("OPEN", residentName, flatNo, true,
                    Instant.now().plus(CACHE_TTL), 0);
        } else {
            return new BarrierDecision("HOLD", null, null, true,
                    Instant.now().plus(Duration.ofMinutes(5)), 0);
        }
    }

    private BarrierDecision fallbackToCache(Long communityId, String credentialType,
                                             String credentialValue, long startNanos) {
        Optional<BarrierValidationCache> cached = cacheRepository
                .findByCommunityIdAndCredentialTypeAndCredentialValue(communityId, credentialType, credentialValue);

        if (cached.isPresent() && cached.get().getValidUntil().isAfter(Instant.now())) {
            BarrierValidationCache entry = cached.get();
            long elapsed = (System.nanoTime() - startNanos) / 1_000_000;
            log.info("DB cache fallback for {}:{} → {} in {}ms", credentialType, credentialValue, entry.getAction(), elapsed);
            return new BarrierDecision(entry.getAction(), entry.getResidentName(), entry.getFlatNo(),
                    false, entry.getValidUntil(), elapsed);
        }

        long elapsed = (System.nanoTime() - startNanos) / 1_000_000;
        log.warn("No cache for {}:{} — HOLD (unknown credential)", credentialType, credentialValue);
        return new BarrierDecision("HOLD", null, null, false, Instant.now(), elapsed);
    }

    @Transactional
    public void syncCacheForCommunity(Long communityId, List<CacheEntry> entries) {
        for (CacheEntry entry : entries) {
            Optional<BarrierValidationCache> existing = cacheRepository
                    .findByCommunityIdAndCredentialTypeAndCredentialValue(
                            communityId, entry.credentialType(), entry.credentialValue());

            BarrierValidationCache cache = existing.orElse(BarrierValidationCache.builder()
                    .communityId(communityId)
                    .credentialType(entry.credentialType())
                    .credentialValue(entry.credentialValue())
                    .build());

            cache.setResidentName(entry.residentName());
            cache.setFlatNo(entry.flatNo());
            cache.setAction(entry.action());
            cache.setValidUntil(Instant.now().plus(CACHE_TTL));
            cache.setSyncedAt(Instant.now());

            cacheRepository.save(cache);

            String cacheKey = communityId + ":" + entry.credentialType() + ":" + entry.credentialValue();
            localHotCache.put(cacheKey, new BarrierDecision(entry.action(), entry.residentName(),
                    entry.flatNo(), true, Instant.now().plus(CACHE_TTL), 0));
        }
        log.info("Synced {} cache entries for community {}", entries.size(), communityId);
    }

    private void updateDbCache(Long communityId, String credentialType, String credentialValue,
                                BarrierDecision decision) {
        try {
            Optional<BarrierValidationCache> existing = cacheRepository
                    .findByCommunityIdAndCredentialTypeAndCredentialValue(communityId, credentialType, credentialValue);

            BarrierValidationCache cache = existing.orElse(BarrierValidationCache.builder()
                    .communityId(communityId)
                    .credentialType(credentialType)
                    .credentialValue(credentialValue)
                    .build());

            cache.setResidentName(decision.residentName());
            cache.setFlatNo(decision.flatNo());
            cache.setAction(decision.action());
            cache.setValidUntil(decision.validUntil());
            cache.setSyncedAt(Instant.now());

            cacheRepository.save(cache);
        } catch (Exception e) {
            log.warn("Failed to update DB cache for {}:{}: {}", credentialType, credentialValue, e.getMessage());
        }
    }

    @Transactional
    public void purgeExpiredCache() {
        cacheRepository.deleteByValidUntilBefore(Instant.now());
    }

    public CacheStats getCacheStats(Long communityId) {
        long dbEntries = cacheRepository.countByCommunityId(communityId);
        long hotEntries = localHotCache.entrySet().stream()
                .filter(e -> e.getKey().startsWith(communityId + ":"))
                .count();
        return new CacheStats(dbEntries, hotEntries);
    }

    public record BarrierDecision(String action, String residentName, String flatNo,
                                   boolean cloudVerified, Instant validUntil, long latencyMs) {
        public BarrierDecision withLatency(long ms) {
            return new BarrierDecision(action, residentName, flatNo, cloudVerified, validUntil, ms);
        }
    }

    public record CacheEntry(String credentialType, String credentialValue,
                              String residentName, String flatNo, String action) {}

    public record CacheStats(long dbCacheEntries, long hotCacheEntries) {}
}
