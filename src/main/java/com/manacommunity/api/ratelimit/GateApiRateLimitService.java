package com.manacommunity.api.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class GateApiRateLimitService {

    private final SlidingWindowRateLimiter partnerLimiter = new SlidingWindowRateLimiter(60, 60_000);
    private final SlidingWindowRateLimiter societyLimiter = new SlidingWindowRateLimiter(200, 60_000);
    private final ConcurrentHashMap<String, Integer> partnerBaseLimits = new ConcurrentHashMap<>();

    private static final int MEAL_HOUR_MULTIPLIER = 3;
    private static final int LUNCH_START = 11;
    private static final int LUNCH_END = 14;
    private static final int DINNER_START = 18;
    private static final int DINNER_END = 22;

    @PostConstruct
    void initDefaults() {
        partnerBaseLimits.put("swiggy", 100);
        partnerBaseLimits.put("zomato", 100);
        partnerBaseLimits.put("dunzo", 60);
        partnerBaseLimits.put("blinkit", 80);
        partnerBaseLimits.put("amazon", 40);
    }

    public RateLimitDecision checkPartnerRate(String partnerId, Long societyId) {
        String partnerKey = "partner:" + partnerId;
        String societyKey = "society:" + societyId;
        String comboKey = partnerId + ":" + societyId;

        applyMealHourBurst(partnerId);

        SlidingWindowRateLimiter.RateLimitResult partnerResult = partnerLimiter.tryAcquire(partnerKey);
        if (!partnerResult.allowed()) {
            log.warn("Partner {} rate-limited: {}/{}", partnerId, partnerResult.currentCount(), partnerResult.limit());
            return new RateLimitDecision(false, "PARTNER_THROTTLED",
                    "Partner " + partnerId + " exceeded rate limit", partnerResult.retryAfterMs(),
                    partnerResult.currentCount(), partnerResult.limit());
        }

        SlidingWindowRateLimiter.RateLimitResult societyResult = societyLimiter.tryAcquire(societyKey);
        if (!societyResult.allowed()) {
            log.warn("Society {} rate-limited for partner {}", societyId, partnerId);
            return new RateLimitDecision(false, "SOCIETY_THROTTLED",
                    "Society " + societyId + " exceeded aggregate rate limit", societyResult.retryAfterMs(),
                    societyResult.currentCount(), societyResult.limit());
        }

        return new RateLimitDecision(true, "ALLOWED", null, 0,
                partnerResult.currentCount(), partnerResult.limit());
    }

    private void applyMealHourBurst(String partnerId) {
        int baseLimit = partnerBaseLimits.getOrDefault(partnerId, 60);
        int hour = LocalTime.now().getHour();
        boolean isMealHour = (hour >= LUNCH_START && hour < LUNCH_END)
                || (hour >= DINNER_START && hour < DINNER_END);

        int effectiveLimit = isMealHour ? baseLimit * MEAL_HOUR_MULTIPLIER : baseLimit;
        partnerLimiter.setBurstLimit("partner:" + partnerId, effectiveLimit);
    }

    public void setPartnerBaseLimit(String partnerId, int limit) {
        partnerBaseLimits.put(partnerId, limit);
    }

    public Map<String, SlidingWindowRateLimiter.RateLimitInfo> getPartnerStats() {
        Map<String, SlidingWindowRateLimiter.RateLimitInfo> stats = new ConcurrentHashMap<>();
        for (String partner : partnerBaseLimits.keySet()) {
            stats.put(partner, partnerLimiter.getInfo("partner:" + partner));
        }
        return stats;
    }

    public SlidingWindowRateLimiter.RateLimitInfo getSocietyStats(Long societyId) {
        return societyLimiter.getInfo("society:" + societyId);
    }

    public record RateLimitDecision(boolean allowed, String code, String message, long retryAfterMs,
                                     int currentCount, int limit) {}
}
