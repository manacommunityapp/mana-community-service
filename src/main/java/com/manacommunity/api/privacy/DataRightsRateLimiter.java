package com.manacommunity.api.privacy;

import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.platform.ratelimit.RedisSlidingWindowRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DPDP Act 2023 — rate-limits data-rights requests per user to prevent abuse.
 *
 * <p>Backed by Redis + Lua sliding window ({@code privacy:rights:rl:user:{userId}})
 * when Redis is available, with automatic fallback to an in-memory sliding window.</p>
 *
 * Default: max 5 requests per 24 hours per user. Configure via:
 *   app.privacy.rights-request.max-per-day (default 5)
 *   app.privacy.rights-request.window-hours (default 24)
 */
@Component
public class DataRightsRateLimiter {

    private static final String REDIS_KEY_PREFIX = "privacy:rights:rl:user:";

    @Value("${app.privacy.rights-request.max-per-day:5}")
    private int maxPerDay = 5;

    @Value("${app.privacy.rights-request.window-hours:24}")
    private int windowHours = 24;

    private final RedisSlidingWindowRateLimiter redisSlidingWindow;
    private final Map<Long, Deque<Instant>> timestamps = new ConcurrentHashMap<>();

    public DataRightsRateLimiter() {
        this(null);
    }

    @Autowired
    public DataRightsRateLimiter(
            @Autowired(required = false) RedisSlidingWindowRateLimiter redisSlidingWindow) {
        this.redisSlidingWindow = redisSlidingWindow;
    }

    /**
     * Checks and records a new request for {@code userId}.
     * Throws HTTP 429 if the sliding-window limit is exceeded.
     */
    public void checkAndRecord(Long userId) {
        if (redisSlidingWindow != null && userId != null) {
            long windowMs = windowHours * 3_600_000L;
            Boolean allowed = redisSlidingWindow.tryAcquire(REDIS_KEY_PREFIX + userId, windowMs, maxPerDay);
            if (allowed != null) {
                if (!allowed) {
                    throw rateLimitExceeded();
                }
                return;
            }
        }

        Instant now = Instant.now();
        Instant cutoff = now.minus(windowHours, ChronoUnit.HOURS);

        timestamps.compute(userId, (id, deque) -> {
            if (deque == null) deque = new ArrayDeque<>();
            // evict entries outside the window
            while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
                deque.pollFirst();
            }
            if (deque.size() >= maxPerDay) {
                throw rateLimitExceeded();
            }
            deque.addLast(now);
            return deque;
        });
    }

    private ManaCommunityException rateLimitExceeded() {
        return new ManaCommunityException(
                "Too many data-rights requests. You may submit at most " + maxPerDay
                        + " request(s) every " + windowHours + " hours.",
                HttpStatus.TOO_MANY_REQUESTS,
                "RATE_LIMIT_EXCEEDED"
        );
    }
}
