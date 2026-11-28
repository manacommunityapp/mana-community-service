package com.manacommunity.api.platform.tenant;

import com.manacommunity.api.security.CommunityMembershipService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Resolves the community (tenant) context from the authenticated user's JWT claims
 * or an authorized {@code X-Community-ID} header validated against {@link CommunityMembershipService}.
 */
@Slf4j
@Component
@Order(200)
public class TenantSecurityFilter extends OncePerRequestFilter {

    private static final String COMMUNITY_ID_HEADER = "X-Community-ID";

    @Autowired(required = false)
    private CommunityMembershipService membershipService;

    @Autowired(required = false)
    private AppUserRepository userRepository;

    public void setMembershipService(CommunityMembershipService membershipService) {
        this.membershipService = membershipService;
    }

    public void setUserRepository(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

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

            Long activeCommunityId = principal.getCommunityId();

            String headerValue = request.getHeader(COMMUNITY_ID_HEADER);
            if (headerValue != null && !headerValue.isBlank()) {
                try {
                    Long requestedCommunityId = Long.parseLong(headerValue.trim());
                    boolean sameAsToken = activeCommunityId != null && activeCommunityId.equals(requestedCommunityId);
                    boolean authorizedMembership = !sameAsToken && isAuthorizedMembership(principal, requestedCommunityId);
                    if (!sameAsToken && !authorizedMembership) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"error\":\"Tenant context mismatch\"," +
                                "\"detail\":\"Requested community does not match your membership\"}");
                        return;
                    }
                    activeCommunityId = requestedCommunityId;
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Invalid X-Community-ID header value\"}");
                    return;
                }
            }

            if (activeCommunityId != null) {
                TenantContextHolder.set(activeCommunityId);
            }

            chain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private boolean isAuthorizedMembership(UserPrincipal principal, Long requestedCommunityId) {
        if (membershipService == null || requestedCommunityId == null) {
            return false;
        }
        AppUser user = principal.getUser();
        if (user == null && userRepository != null && principal.getId() != null) {
            user = userRepository.findById(principal.getId()).orElse(null);
        }
        return user != null && membershipService.isUserAuthorizedForCommunity(user, requestedCommunityId);
    }
}
