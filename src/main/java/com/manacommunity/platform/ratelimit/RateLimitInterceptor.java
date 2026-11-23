package com.manacommunity.platform.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final DistributedRateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod method)) return true;

        RateLimit annotation = method.getMethodAnnotation(RateLimit.class);
        if (annotation == null) return true;

        String ip = getIp(request);
        String accountKey = getAccountKey(request);
        DistributedRateLimitService.RateLimitResult result =
                rateLimitService.check(ip, accountKey, annotation.type());

        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.resetAtEpochSecond()));

        if (!result.allowed()) {
            long retryAfter = result.resetAtEpochSecond() - System.currentTimeMillis() / 1000;
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(Math.max(1, retryAfter)));
            response.getWriter().write("{\"error\":\"Too many requests\",\"retryAfterSeconds\":"
                    + Math.max(1, retryAfter) + ",\"type\":\"" + annotation.type().name() + "\"}");
            log.warn("Rate limit exceeded: type={} ip={}", annotation.type(), ip);
            return false;
        }
        return true;
    }

    private String getIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    private String getAccountKey(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        return principal != null ? principal.getName() : "anonymous:" + getIp(request);
    }
}
