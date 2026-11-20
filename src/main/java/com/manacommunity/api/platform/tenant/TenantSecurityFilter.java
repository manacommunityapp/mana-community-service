package com.manacommunity.api.platform.tenant;

import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Resolves the community (tenant) context from the authenticated user's JWT claims.
 *
 * <p><strong>Security invariant</strong>: the {@code X-Community-ID} header is a
 * <em>context-selection hint</em>, not a permission grant. If a request includes the
 * header with a community ID that does not match the authenticated user's membership,
 * it is rejected with 403 — the header never allows access to another community's data.
 *
 * <p>Runs AFTER Spring Security's JWT filter (Order 200) so the SecurityContext is
 * already populated when this filter executes. Cleared in finally to avoid leaking
 * across pooled threads.
 */
@Slf4j
@Component
@Order(200)
public class TenantSecurityFilter extends OncePerRequestFilter {

    private static final String COMMUNITY_ID_HEADER = "X-Community-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
                // Unauthenticated — public endpoint, skip tenant resolution
                chain.doFilter(request, response);
                return;
            }

            Long jwtCommunityId = principal.getCommunityId();

            String headerValue = request.getHeader(COMMUNITY_ID_HEADER);
            if (headerValue != null && !headerValue.isBlank()) {
                try {
                    Long requestedCommunityId = Long.parseLong(headerValue.trim());
                    if (jwtCommunityId == null || !jwtCommunityId.equals(requestedCommunityId)) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"error\":\"Tenant context mismatch\"," +
                                "\"detail\":\"Requested community does not match your membership\"}");
                        return;
                    }
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Invalid X-Community-ID header value\"}");
                    return;
                }
            }

            if (jwtCommunityId != null) {
                TenantContextHolder.set(jwtCommunityId);
            }

            chain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
