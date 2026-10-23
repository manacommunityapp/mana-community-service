package com.manacommunity.api.privacy;

import com.manacommunity.api.exception.ManaCommunityException;
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
 * Uses an in-memory sliding-window counter (no external dependency). Suitable
 * for a single-instance deployment; replace with Redis + Lua if horizontal
 * scaling is required.
 *
 * Default: max 5 requests per 24 hours per user. Configure via:
 *   app.privacy.rights-request.max-per-day (default 5)
 *   app.privacy.rights-request.window-hours (default 24)
 */
@Component
public class DataRightsRateLimiter {

    @Value("${app.privacy.rights-request.max-per-day:5}")
    private int maxPerDay;

    @Value("${app.privacy.rights-request.window-hours:24}")
    private int windowHours;

    private final Map<Long, Deque<Instant>> timestamps = new ConcurrentHashMap<>();

    /**
     * Checks and records a new request for {@code userId}.
     * Throws HTTP 429 if the sliding-window limit is exceeded.
     */
    public void checkAndRecord(Long userId) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(windowHours, ChronoUnit.HOURS);

        timestamps.compute(userId, (id, deque) -> {
            if (deque == null) deque = new ArrayDeque<>();
            // evict entries outside the window
            while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
                deque.pollFirst();
            }
            if (deque.size() >= maxPerDay) {
                throw new ManaCommunityException(
                    "Too many data-rights requests. You may submit at most " + maxPerDay
                        + " request(s) every " + windowHours + " hours.",
                    HttpStatus.TOO_MANY_REQUESTS,
                    "RATE_LIMIT_EXCEEDED"
                );
            }
            deque.addLast(now);
            return deque;
        });
    }
}
