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
    private static final String TOWER_ID_HEADER = "X-Tower-Id";
    private static final String FLAT_ID_HEADER = "X-Flat-Id";
    private static final String PROPERTY_ID_HEADER = "X-Property-Id";

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
        boolean boundLegacyTenantContext = false;
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
                Long activeFlatId = principal.getFlatId();
                Long activeTowerId = principal.getTowerId();
                AppUser resolvedUser = resolveUser(principal);

                String flatHeader = request.getHeader(FLAT_ID_HEADER);
                if (flatHeader == null || flatHeader.isBlank()) {
                    flatHeader = request.getHeader(PROPERTY_ID_HEADER);
                }
                if (flatHeader != null && !flatHeader.isBlank()) {
                    try {
                        Long requestedFlatId = Long.parseLong(flatHeader.trim());
                        boolean sameFlat = activeFlatId != null && activeFlatId.equals(requestedFlatId);
                        boolean allowedFlat = sameFlat || (membershipService != null && resolvedUser != null
                                && membershipService.isUserAuthorizedForProperty(resolvedUser, activeCommunityId, requestedFlatId));
                        if (!allowedFlat) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"error\":\"Flat context mismatch\"," +
                                    "\"detail\":\"Requested flat does not match your membership\"}");
                            return;
                        }
                        activeFlatId = requestedFlatId;
                    } catch (NumberFormatException e) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Invalid X-Flat-Id header value\"}");
                        return;
                    }
                }

                String towerHeader = request.getHeader(TOWER_ID_HEADER);
                if (towerHeader != null && !towerHeader.isBlank()) {
                    try {
                        Long requestedTowerId = Long.parseLong(towerHeader.trim());
                        boolean sameTower = activeTowerId != null && activeTowerId.equals(requestedTowerId);
                        boolean allowedTower = sameTower || (membershipService != null && resolvedUser != null
                                && membershipService.isUserAuthorizedForTower(resolvedUser, activeCommunityId, requestedTowerId));
                        if (!allowedTower) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"error\":\"Tower context mismatch\"," +
                                    "\"detail\":\"Requested tower does not match your membership\"}");
                            return;
                        }
                        activeTowerId = requestedTowerId;
                    } catch (NumberFormatException e) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Invalid X-Tower-Id header value\"}");
                        return;
                    }
                }

                TenantContextHolder.setContext(activeCommunityId, activeTowerId, activeFlatId, principal.getId());
                if (!com.manacommunity.api.config.tenant.TenantContext.isSet()) {
                    Long orgId = resolvedUser != null ? resolvedUser.getOrganizationId() : null;
                    com.manacommunity.api.config.tenant.TenantContext.setTenant(orgId, activeCommunityId);
                    com.manacommunity.api.config.tenant.TenantContext.setUnitContext(
                            activeTowerId, null, activeFlatId, principal.getId());
                    boundLegacyTenantContext = true;
                }
            }

            chain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
            if (boundLegacyTenantContext) {
                com.manacommunity.api.config.tenant.TenantContext.clear();
            }
        }
    }

    private boolean isAuthorizedMembership(UserPrincipal principal, Long requestedCommunityId) {
        if (membershipService == null || requestedCommunityId == null) {
            return false;
        }
        AppUser user = resolveUser(principal);
        return user != null && membershipService.isUserAuthorizedForCommunity(user, requestedCommunityId);
    }

    private AppUser resolveUser(UserPrincipal principal) {
        AppUser user = principal.getUser();
        if (user == null && userRepository != null && principal.getId() != null) {
            user = userRepository.findById(principal.getId()).orElse(null);
        }
        return user;
    }
}
