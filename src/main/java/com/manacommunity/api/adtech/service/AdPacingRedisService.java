package com.manacommunity.api.adtech.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Low-latency Redis-backed pacing, frequency capping, and impression deduplication.
 * Uses atomic Lua scripts on Redis Cluster; falls back cleanly to local atomic maps
 * when Redis is unavailable (e.g., unit tests or local profile without Redis).
 */
@Slf4j
@Service
public class AdPacingRedisService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final long TTL_24H_SECONDS = 86_400L;

    private static final DefaultRedisScript<Long> FREQ_CAP_SCRIPT = new DefaultRedisScript<>(
            """
            local key = KEYS[1]
            local cap = tonumber(ARGV[1])
            local ttl = tonumber(ARGV[2])
            local current = redis.call('GET', key)
            if current and tonumber(current) >= cap then
                return -1
            end
            local newVal = redis.call('INCR', key)
            if newVal == 1 then
                redis.call('EXPIRE', key, ttl)
            end
            return newVal
            """,
            Long.class
    );

    private static final DefaultRedisScript<Long> TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>(
            """
            local key        = KEYS[1]
            local refillRate = tonumber(ARGV[1])
            local burstCap   = tonumber(ARGV[2])
            local cost       = tonumber(ARGV[3])
            local now        = tonumber(ARGV[4])
            local bucket = redis.call('HMGET', key, 'tokens', 'lastRefill')
            local tokens     = tonumber(bucket[1]) or burstCap
            local lastRefill = tonumber(bucket[2]) or now
            local elapsed = math.max(0, now - lastRefill)
            local refilled = math.min(burstCap, tokens + elapsed * refillRate)
            if refilled < cost then
                return 0
            end
            redis.call('HMSET', key, 'tokens', refilled - cost, 'lastRefill', now)
            redis.call('EXPIRE', key, 86400)
            return 1
            """,
            Long.class
    );

    private final StringRedisTemplate redis;

    // Local fallbacks (used only if Redis is absent/unreachable)
    private final Map<String, AtomicInteger> localFreqCaps = new ConcurrentHashMap<>();
    private final Map<String, LocalBucket> localBuckets = new ConcurrentHashMap<>();
    private final Set<String> localImpressions = ConcurrentHashMap.newKeySet();
    private final Set<String> localHaltedCampaigns = ConcurrentHashMap.newKeySet();

    public AdPacingRedisService(ObjectProvider<StringRedisTemplate> redisProvider) {
        this.redis = redisProvider.getIfAvailable();
    }

    /**
     * Atomically checks and increments daily frequency cap for a resident + campaign.
     *
     * @return remaining impressions today (>= 0), or -1 if cap already reached
     */
    public int tryAcquireFrequencyCap(String campaignId, String userId, int capPerDay) {
        String day = LocalDate.now(IST).format(DAY_FMT);
        String key = "fc:" + campaignId + ":" + userId + ":" + day;

        if (redis != null) {
            try {
                Long newCount = redis.execute(
                        FREQ_CAP_SCRIPT,
                        List.of(key),
                        String.valueOf(capPerDay),
                        String.valueOf(TTL_24H_SECONDS)
                );
                if (newCount == null || newCount < 0) {
                    return -1;
                }
                return Math.max(0, capPerDay - newCount.intValue());
            } catch (Exception ex) {
                log.debug("Redis freq-cap fallback for {}: {}", key, ex.getMessage());
            }
        }

        AtomicInteger counter = localFreqCaps.computeIfAbsent(key, k -> new AtomicInteger(0));
        while (true) {
            int current = counter.get();
            if (current >= capPerDay) {
                return -1;
            }
            if (counter.compareAndSet(current, current + 1)) {
                return Math.max(0, capPerDay - (current + 1));
            }
        }
    }

    /**
     * Atomically checks and consumes 1 impression token from the campaign's token bucket.
     */
    public boolean tryConsumeBudgetToken(String campaignId, long totalBudgetPaise, long cpmPaise, Instant startAt, Instant endAt) {
        if (isCampaignHalted(campaignId)) {
            return false;
        }
        long durationSeconds = Math.max(1L, endAt.getEpochSecond() - startAt.getEpochSecond());
        double costPerImpPaise = Math.max(1.0, cpmPaise / 1000.0);
        double totalImpressions = Math.max(1.0, totalBudgetPaise / costPerImpPaise);
        double refillRate = Math.max(0.01, totalImpressions / durationSeconds);
        double burstCap = Math.max(10.0, refillRate * 10.0);
        long nowSec = Instant.now().getEpochSecond();

        String key = "bp:" + campaignId;
        if (redis != null) {
            try {
                Long ok = redis.execute(
                        TOKEN_BUCKET_SCRIPT,
                        List.of(key),
                        String.valueOf(refillRate),
                        String.valueOf(burstCap),
                        "1",
                        String.valueOf(nowSec)
                );
                return ok != null && ok == 1L;
            } catch (Exception ex) {
                log.debug("Redis token-bucket fallback for {}: {}", campaignId, ex.getMessage());
            }
        }

        LocalBucket bucket = localBuckets.computeIfAbsent(key, k -> new LocalBucket(burstCap, nowSec));
        return bucket.tryConsume(refillRate, burstCap, 1.0, nowSec);
    }

    /**
     * Redis SET NX deduplication for client impression retries.
     *
     * @return true if this impressionId is new, false if already seen
     */
    public boolean markImpressionIfNew(String impressionId) {
        String key = "imp:" + impressionId;
        if (redis != null) {
            try {
                Boolean added = redis.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(TTL_24H_SECONDS));
                return Boolean.TRUE.equals(added);
            } catch (Exception ex) {
                log.debug("Redis impression-dedup fallback for {}: {}", impressionId, ex.getMessage());
            }
        }
        return localImpressions.add(impressionId);
    }

    public void haltCampaign(String campaignId) {
        localHaltedCampaigns.add(campaignId);
        if (redis != null) {
            try {
                redis.opsForValue().set("bp:halt:" + campaignId, "1", Duration.ofDays(30));
            } catch (Exception ex) {
                log.debug("Redis halt set fallback for {}: {}", campaignId, ex.getMessage());
            }
        }
    }

    public void unhaltCampaign(String campaignId) {
        localHaltedCampaigns.remove(campaignId);
        if (redis != null) {
            try {
                redis.delete("bp:halt:" + campaignId);
            } catch (Exception ex) {
                log.debug("Redis halt delete fallback for {}: {}", campaignId, ex.getMessage());
            }
        }
    }

    public boolean isCampaignHalted(String campaignId) {
        if (localHaltedCampaigns.contains(campaignId)) {
            return true;
        }
        if (redis != null) {
            try {
                return Boolean.TRUE.equals(redis.hasKey("bp:halt:" + campaignId));
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    private static final class LocalBucket {
        private double tokens;
        private long lastRefill;

        private LocalBucket(double initialTokens, long nowSec) {
            this.tokens = initialTokens;
            this.lastRefill = nowSec;
        }

        synchronized boolean tryConsume(double refillRate, double burstCap, double cost, long nowSec) {
            long elapsed = Math.max(0L, nowSec - lastRefill);
            double refilled = Math.min(burstCap, tokens + elapsed * refillRate);
            if (refilled < cost) {
                tokens = refilled;
                lastRefill = nowSec;
                return false;
            }
            tokens = refilled - cost;
            lastRefill = nowSec;
            return true;
        }
    }
}
