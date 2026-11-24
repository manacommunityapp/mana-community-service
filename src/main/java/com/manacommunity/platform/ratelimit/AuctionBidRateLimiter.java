package com.manacommunity.platform.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-actor auction bid rate limiter to prevent double-click/scripted bid flooding
 * on both Event item auctions (WebSocket) and Sports auctions (REST/WebSocket).
 *
 * <p>Uses {@link RedisSlidingWindowRateLimiter} when Redis is available, with an
 * in-memory cooldown map fallback when Redis is unavailable.</p>
 */
@Slf4j
@Component
public class AuctionBidRateLimiter {

    private static final String EVENT_KEY_PREFIX  = "auction:rl:event:";
    private static final String SPORTS_KEY_PREFIX = "auction:rl:sports:";

    @Value("${app.auction.bid-cooldown-ms:2000}")
    private long bidCooldownMs = 2000L;

    @Value("${app.auction.max-bids-per-window:1}")
    private int maxBidsPerWindow = 1;

    private final RedisSlidingWindowRateLimiter redisSlidingWindow;
    private final ConcurrentHashMap<String, Long> localCooldowns = new ConcurrentHashMap<>();

    public AuctionBidRateLimiter() {
        this(null);
    }

    @Autowired
    public AuctionBidRateLimiter(
            @Autowired(required = false) RedisSlidingWindowRateLimiter redisSlidingWindow) {
        this.redisSlidingWindow = redisSlidingWindow;
    }

    /** Package/test constructor with explicit window and limit. */
    public AuctionBidRateLimiter(RedisSlidingWindowRateLimiter redisSlidingWindow, long bidCooldownMs, int maxBidsPerWindow) {
        this.redisSlidingWindow = redisSlidingWindow;
        this.bidCooldownMs = bidCooldownMs;
        this.maxBidsPerWindow = maxBidsPerWindow;
    }

    /**
     * Checks whether a user may place a bid on an Event auction item.
     */
    public boolean tryAcquireEventBid(Long itemId, Long userId) {
        if (itemId == null || userId == null || bidCooldownMs <= 0) return true;
        return tryAcquire(EVENT_KEY_PREFIX + itemId + ":user:" + userId);
    }

    /**
     * Checks whether a team may place a bid in a Sports auction config.
     * Keyed by team so an auctioneer can still record bids from different teams rapidly.
     */
    public boolean tryAcquireSportsBid(Long configId, Long teamId) {
        if (configId == null || teamId == null || bidCooldownMs <= 0) return true;
        return tryAcquire(SPORTS_KEY_PREFIX + configId + ":team:" + teamId);
    }

    private boolean tryAcquire(String key) {
        if (redisSlidingWindow != null) {
            Boolean allowed = redisSlidingWindow.tryAcquire(key, bidCooldownMs, maxBidsPerWindow);
            if (allowed != null) {
                return allowed;
            }
        }
        return tryAcquireInMemory(key);
    }

    private boolean tryAcquireInMemory(String key) {
        long now = System.currentTimeMillis();
        final boolean[] allowed = {false};
        localCooldowns.compute(key, (k, lastTs) -> {
            if (lastTs == null || (now - lastTs) >= bidCooldownMs) {
                allowed[0] = true;
                return now;
            }
            return lastTs;
        });
        // Opportunistic bound if map grows large
        if (localCooldowns.size() > 10_000) {
            localCooldowns.entrySet().removeIf(e -> (now - e.getValue()) >= bidCooldownMs);
        }
        return allowed[0];
    }
}
