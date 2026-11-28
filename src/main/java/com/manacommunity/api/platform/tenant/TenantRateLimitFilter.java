package com.manacommunity.api.platform.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Gateway filter enforcing tenant-level API rate quotas.
 * Positioned immediately after {@link TenantSecurityFilter} (Order 205).
 */
@Slf4j
@Component
@Order(205)
@RequiredArgsConstructor
public class TenantRateLimitFilter extends OncePerRequestFilter {

    private final TenantRateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Long communityId = TenantContextHolder.get();

        if (communityId != null && communityId > 0L) {
            TenantRateLimitService.TenantRateLimitResult decision = rateLimitService.checkTenant(communityId);

            response.setHeader("X-RateLimit-Tenant-Limit", String.valueOf(decision.limit()));
            response.setHeader("X-RateLimit-Tenant-Remaining", String.valueOf(decision.remaining()));
            response.setHeader("X-RateLimit-Tenant-Reset", String.valueOf(decision.resetEpochSecond()));

            if (!decision.allowed()) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", "60");
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(String.format(
                        "{\"error\":\"TENANT_RATE_LIMIT_EXCEEDED\",\"communityId\":%d,\"message\":\"Community request quota exceeded (%d req/min). Fair-share protection engaged.\",\"retryAfter\":60}",
                        communityId, decision.limit()
                ));
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
