package com.manacommunity.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Strict per-endpoint rate limiter for authentication endpoints. Prevents
 * brute-force password/OTP guessing and credential-stuffing attacks.
 *
 * <p>Each endpoint has its own limit and window. After the limit is hit,
 * subsequent requests receive 429 with a {@code Retry-After} header whose
 * value doubles on each consecutive blocked request (exponential backoff,
 * capped at 1 hour).</p>
 *
 * <p>Runs at highest precedence + 1, right after the global {@link RateLimitFilter}.</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_KEYS = 100_000;
    private static final long MAX_BACKOFF_SEC = 3600L;

    private final boolean enabled;
    private final int loginLimit;
    private final long loginWindowMs;
    private final int registerLimit;
    private final long registerWindowMs;
    private final int forgotPasswordLimit;
    private final long forgotPasswordWindowMs;
    private final int otpLimit;
    private final long otpWindowMs;
    private final boolean trustForwardedHeader;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(
            @Value("${app.security.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.security.auth-rate-limit.login.limit:5}") int loginLimit,
            @Value("${app.security.auth-rate-limit.login.window-minutes:15}") int loginWindowMin,
            @Value("${app.security.auth-rate-limit.register.limit:3}") int registerLimit,
            @Value("${app.security.auth-rate-limit.register.window-minutes:60}") int registerWindowMin,
            @Value("${app.security.auth-rate-limit.forgot-password.limit:3}") int forgotPasswordLimit,
            @Value("${app.security.auth-rate-limit.forgot-password.window-minutes:60}") int forgotPasswordWindowMin,
            @Value("${app.security.auth-rate-limit.otp.limit:5}") int otpLimit,
            @Value("${app.security.auth-rate-limit.otp.window-minutes:15}") int otpWindowMin,
            @Value("${app.security.rate-limit.trust-forwarded-header:false}") boolean trustForwardedHeader) {
        this.enabled = enabled;
        this.loginLimit = loginLimit;
        this.loginWindowMs = loginWindowMin * 60_000L;
        this.registerLimit = registerLimit;
        this.registerWindowMs = registerWindowMin * 60_000L;
        this.forgotPasswordLimit = forgotPasswordLimit;
        this.forgotPasswordWindowMs = forgotPasswordWindowMin * 60_000L;
        this.otpLimit = otpLimit;
        this.otpWindowMs = otpWindowMin * 60_000L;
        this.trustForwardedHeader = trustForwardedHeader;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!enabled || !"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();
        EndpointRule rule = resolveRule(uri);
        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = clientIp(request);
        String bucketKey = rule.name + ":" + clientIp;
        long now = System.currentTimeMillis();

        evictIfNeeded(now);

        Bucket bucket = buckets.compute(bucketKey, (k, existing) -> {
            if (existing == null || now - existing.windowStart >= rule.windowMs) {
                return new Bucket(now);
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (bucket.count.get() > rule.limit) {
            int consecutiveBlocks = bucket.consecutiveBlocks.incrementAndGet();
            long baseRetry = Math.max(1, (rule.windowMs - (now - bucket.windowStart)) / 1000);
            long backoff = Math.min(MAX_BACKOFF_SEC, baseRetry * (1L << Math.min(consecutiveBlocks - 1, 10)));
            writeTooManyRequests(request, response, backoff, rule.name);
            log.warn("Auth rate limit hit: endpoint={} ip={} attempts={} window={}min",
                    rule.name, clientIp, bucket.count.get(), rule.windowMs / 60_000);
            return;
        }

        chain.doFilter(request, response);
    }

    private EndpointRule resolveRule(String uri) {
        if (uri.equals("/api/auth/login")) {
            return new EndpointRule("login", loginLimit, loginWindowMs);
        }
        if (uri.equals("/api/auth/register")) {
            return new EndpointRule("register", registerLimit, registerWindowMs);
        }
        if (uri.equals("/api/auth/forgot-password")) {
            return new EndpointRule("forgot-password", forgotPasswordLimit, forgotPasswordWindowMs);
        }
        if (uri.equals("/api/auth/send-signup-otp") || uri.equals("/api/auth/verify-signup-otp")
                || uri.startsWith("/api/otp/")) {
            return new EndpointRule("otp", otpLimit, otpWindowMs);
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        if (trustForwardedHeader) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                int comma = forwarded.indexOf(',');
                return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
            }
        }
        return request.getRemoteAddr();
    }

    private void evictIfNeeded(long now) {
        if (buckets.size() > MAX_TRACKED_KEYS) {
            buckets.entrySet().removeIf(e -> {
                Bucket b = e.getValue();
                return now - b.windowStart > MAX_BACKOFF_SEC * 1000;
            });
        }
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response,
                                      long retryAfterSeconds, String endpoint) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        String safePath = request.getRequestURI()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "");
        String body = "{"
                + "\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"status\":429,"
                + "\"error\":\"RATE_LIMIT_EXCEEDED\","
                + "\"message\":\"Too many " + endpoint + " attempts. Please try again after "
                + retryAfterSeconds + " seconds.\","
                + "\"retryAfter\":" + retryAfterSeconds + ","
                + "\"path\":\"" + safePath + "\"}";
        response.getWriter().write(body);
    }

    private record EndpointRule(String name, int limit, long windowMs) {}

    private static final class Bucket {
        final long windowStart;
        final AtomicInteger count;
        final AtomicInteger consecutiveBlocks;

        Bucket(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(1);
            this.consecutiveBlocks = new AtomicInteger(0);
        }
    }
}
