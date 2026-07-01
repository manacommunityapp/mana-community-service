package com.manacommunity.api.ai.config;

import com.manacommunity.api.security.UserPrincipal;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiter specifically for AI chat endpoints ({@code /api/ai/**}).
 *
 * <p>Uses a sliding-window counter per user ID. Defaults: 20 requests/minute,
 * 200 requests/hour. Configurable via {@code app.ai.rate-limit.*}.</p>
 *
 * <p>Returns HTTP 429 with a {@code Retry-After} header when limits are exceeded.
 * Admins get 2x the limit. Unauthenticated requests are blocked entirely.</p>
 */
@Slf4j
@Component
public class AiRateLimitFilter implements Filter {

    @Value("${app.ai.rate-limit.per-minute:20}")
    private int perMinuteLimit;

    @Value("${app.ai.rate-limit.per-hour:200}")
    private int perHourLimit;

    @Value("${app.ai.rate-limit.admin-multiplier:2}")
    private int adminMultiplier;

    private final Map<Long, UserWindow> windows = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // Only apply to AI endpoints
        if (!request.getRequestURI().startsWith("/api/ai/")) {
            chain.doFilter(req, res);
            return;
        }

        // Only rate-limit POST (not OPTIONS/preflight)
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        // Resolve user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("{\"error\":\"Authentication required for AI chat\"}");
            return;
        }

        Long userId = principal.getId();
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN"));

        int minuteLimit = isAdmin ? perMinuteLimit * adminMultiplier : perMinuteLimit;
        int hourLimit = isAdmin ? perHourLimit * adminMultiplier : perHourLimit;

        UserWindow window = windows.computeIfAbsent(userId, k -> new UserWindow());
        long now = System.currentTimeMillis();

        // Check per-minute
        if (window.isMinuteExceeded(now, minuteLimit)) {
            long retryAfter = 60 - ((now - window.minuteStart.get()) / 1000);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(Math.max(retryAfter, 1)));
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Rate limit exceeded. Max " + minuteLimit
                    + " AI requests per minute. Try again in " + retryAfter + "s.\"}");
            log.warn("AI rate limit (per-minute) exceeded for user={}", userId);
            return;
        }

        // Check per-hour
        if (window.isHourExceeded(now, hourLimit)) {
            long retryAfter = 3600 - ((now - window.hourStart.get()) / 1000);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(Math.max(retryAfter, 1)));
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Hourly rate limit exceeded. Max " + hourLimit
                    + " AI requests per hour.\"}");
            log.warn("AI rate limit (per-hour) exceeded for user={}", userId);
            return;
        }

        // Increment and pass through
        window.increment(now);
        response.setHeader("X-AI-RateLimit-Remaining",
                String.valueOf(minuteLimit - window.minuteCount.get()));

        chain.doFilter(req, res);
    }

    /** Sliding-window counter per user. */
    private static class UserWindow {
        final AtomicLong minuteStart = new AtomicLong(System.currentTimeMillis());
        final AtomicLong hourStart = new AtomicLong(System.currentTimeMillis());
        final AtomicInteger minuteCount = new AtomicInteger(0);
        final AtomicInteger hourCount = new AtomicInteger(0);

        boolean isMinuteExceeded(long now, int limit) {
            if (now - minuteStart.get() > 60_000) {
                minuteStart.set(now);
                minuteCount.set(0);
            }
            return minuteCount.get() >= limit;
        }

        boolean isHourExceeded(long now, int limit) {
            if (now - hourStart.get() > 3_600_000) {
                hourStart.set(now);
                hourCount.set(0);
            }
            return hourCount.get() >= limit;
        }

        void increment(long now) {
            if (now - minuteStart.get() > 60_000) {
                minuteStart.set(now);
                minuteCount.set(0);
            }
            if (now - hourStart.get() > 3_600_000) {
                hourStart.set(now);
                hourCount.set(0);
            }
            minuteCount.incrementAndGet();
            hourCount.incrementAndGet();
        }
    }
}
