package com.manacommunity.api.security;

import com.manacommunity.platform.ratelimit.DistributedRateLimitService;
import com.manacommunity.platform.ratelimit.DistributedRateLimitService.RateLimitResult;
import com.manacommunity.platform.ratelimit.RateLimitType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Strict per-endpoint rate limiter for authentication endpoints.
 * Uses {@link DistributedRateLimitService} (Redis-backed) so counters are
 * consistent across multiple application instances. Falls back to in-memory
 * when Redis is unavailable — limits are still enforced, never skipped.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final boolean bypassLocalhost;
    private final boolean trustForwardedHeader;
    private final DistributedRateLimitService rateLimitService;

    public AuthRateLimitFilter(
            @Value("${app.security.auth-rate-limit.enabled:${app.security.rate-limit.enabled:true}}") boolean enabled,
            @Value("${app.security.auth-rate-limit.bypass-localhost:true}") boolean bypassLocalhost,
            @Value("${app.security.rate-limit.trust-forwarded-header:false}") boolean trustForwardedHeader,
            @Autowired(required = false) DistributedRateLimitService rateLimitService) {
        this.enabled = enabled;
        this.bypassLocalhost = bypassLocalhost;
        this.trustForwardedHeader = trustForwardedHeader;
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!enabled || rateLimitService == null || !"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();
        RateLimitType type = resolveType(uri);
        if (type == null) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = clientIp(request);
        if (bypassLocalhost && isLoopback(clientIp)) {
            chain.doFilter(request, response);
            return;
        }

        // Extract account key from request body hint (email in param) or fall back to IP
        String accountKey = resolveAccountKey(request, clientIp);
        RateLimitResult result = rateLimitService.check(clientIp, accountKey, type);

        if (!result.allowed()) {
            long retryAfter = result.resetAtEpochSecond() - System.currentTimeMillis() / 1000;
            long backoff = Math.max(1, retryAfter);
            writeTooManyRequests(request, response, backoff, type.name().toLowerCase());
            log.warn("Auth rate limit hit: endpoint={} ip={} remaining={}", type, clientIp, result.remaining());
            return;
        }

        chain.doFilter(request, response);
    }

    private RateLimitType resolveType(String uri) {
        if (uri.equals("/api/auth/login")) return RateLimitType.LOGIN;
        if (uri.equals("/api/auth/register")) return RateLimitType.REGISTRATION;
        if (uri.equals("/api/auth/forgot-password")) return RateLimitType.PASSWORD_RESET;
        if (uri.equals("/api/auth/send-signup-otp") || uri.equals("/api/auth/verify-signup-otp")
                || uri.startsWith("/api/otp/")) return RateLimitType.OTP;
        return null;
    }

    private String resolveAccountKey(HttpServletRequest request, String fallbackIp) {
        // Use email parameter as account key when present (before body is consumed)
        String email = request.getParameter("email");
        if (email != null && !email.isBlank()) return email.toLowerCase().trim();
        return fallbackIp;
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

    private boolean isLoopback(String ip) {
        if (ip == null || ip.isBlank()) return false;
        String trimmed = ip.trim();
        return "127.0.0.1".equals(trimmed) || "0:0:0:0:0:0:0:1".equals(trimmed)
                || "::1".equals(trimmed) || "localhost".equalsIgnoreCase(trimmed);
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response,
                                      long retryAfterSeconds, String endpoint) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        String safePath = request.getRequestURI()
                .replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "").replace("\n", "");
        response.getWriter().write("{"
                + "\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"status\":429,"
                + "\"error\":\"RATE_LIMIT_EXCEEDED\","
                + "\"message\":\"Too many " + endpoint + " attempts. Please try again after "
                + retryAfterSeconds + " seconds.\","
                + "\"retryAfterSeconds\":" + retryAfterSeconds + ","
                + "\"type\":\"" + endpoint.toUpperCase() + "\","
                + "\"path\":\"" + safePath + "\"}");
    }
}
