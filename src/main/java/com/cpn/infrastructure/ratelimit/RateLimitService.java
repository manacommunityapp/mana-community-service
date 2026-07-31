package com.cpn.infrastructure.ratelimit;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitService {

    private static class RateWindow {
        final long windowStartMinute;
        final AtomicInteger count = new AtomicInteger(0);

        RateWindow(long windowStartMinute) {
            this.windowStartMinute = windowStartMinute;
        }
    }

    private final Map<String, RateWindow> cache = new ConcurrentHashMap<>();
    private final int maxRequestsPerMinute = 100;

    public boolean tryConsume(String key) {
        long currentMinuteWindow = Instant.now().getEpochSecond() / 60;

        RateWindow window = cache.compute(key, (k, existing) -> {
            if (existing == null || existing.windowStartMinute != currentMinuteWindow) {
                RateWindow newWin = new RateWindow(currentMinuteWindow);
                newWin.count.set(1);
                return newWin;
            }
            existing.count.incrementAndGet();
            return existing;
        });

        return window.count.get() <= maxRequestsPerMinute;
    }
}
