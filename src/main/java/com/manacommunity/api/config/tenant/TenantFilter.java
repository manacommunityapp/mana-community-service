package com.manacommunity.api.config.tenant;

import com.manacommunity.api.security.CommunityMembershipService;
import com.manacommunity.api.security.SecurityErrorWriter;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Enforces the authoritative request-resolution sequence for tenancy:
 * <pre>
 * JWT / authenticated user
 *           |
 *           v
 * Membership and selected community
 *           |
 *           v
 * Server-side authorization
 *           |
 *           v
 * Resolved TenantContext
 *           |
 *           v
 * Data access and business operation
 * </pre>
 * <p>
 * Client headers ({@code X-Community-Id}, {@code X-Tenant-ID}, {@code X-Property-Id})
 * are validated against the user's verified memberships. Unauthorized attempts
 * to access another community are immediately rejected with HTTP 403 Forbidden.
 * Context cleanup is guaranteed in {@code finally} after every request.
 */
@Slf4j
@Component
public class TenantFilter extends OncePerRequestFilter {

    public static final String ORG_HEADER = "X-Organization-Id";
    public static final String COMMUNITY_HEADER = "X-Community-Id";
    public static final String LEGACY_TENANT_HEADER = "X-Tenant-ID";
    public static final String PROPERTY_HEADER = "X-Property-Id";
    public static final String FLAT_HEADER = "X-Flat-Id";

    private final CommunityMembershipService membershipService;
    private final SecurityErrorWriter securityErrorWriter;
    private final AppUserRepository userRepository;

    public TenantFilter(CommunityMembershipService membershipService,
                        SecurityErrorWriter securityErrorWriter,
                        AppUserRepository userRepository) {
        this.membershipService = membershipService;
        this.securityErrorWriter = securityErrorWriter;
        this.userRepository = userRepository;
    }

    public TenantFilter() {
        this(null, null, null);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            AppUser authenticatedUser = resolveAuthenticatedUser(auth);

            Long requestedCommunityId = parseLongHeader(request, COMMUNITY_HEADER);
            if (requestedCommunityId == null) {
                String legacyTenant = request.getHeader(LEGACY_TENANT_HEADER);
                if (legacyTenant != null && legacyTenant.trim().toLowerCase().startsWith("community_")) {
                    try {
                        requestedCommunityId = Long.parseLong(legacyTenant.trim().substring(10));
                    } catch (NumberFormatException ignored) {}
                }
            }

            Long requestedPropertyId = parseLongHeader(request, PROPERTY_HEADER);
            if (requestedPropertyId == null) {
                requestedPropertyId = parseLongHeader(request, FLAT_HEADER);
            }

            Long resolvedCommunityId = null;
            Long resolvedOrgId = null;
            Long resolvedUserId = null;
            Long resolvedPropertyId = null;

            if (authenticatedUser != null) {
                resolvedUserId = authenticatedUser.getId();

                // 1. Authorize requested community against server-side memberships
                if (requestedCommunityId != null) {
                    if (membershipService != null && !membershipService.isUserAuthorizedForCommunity(authenticatedUser, requestedCommunityId)) {
                        log.warn("Access denied: user {} is not authorized for requested community {}",
                                authenticatedUser.getId(), requestedCommunityId);
                        sendForbidden(request, response, "TENANT_ACCESS_DENIED",
                                "User is not authorized for the requested community: " + requestedCommunityId);
                        return;
                    }
                    resolvedCommunityId = requestedCommunityId;
                } else {
                    // Default to user's assigned community
                    resolvedCommunityId = membershipService != null
                            ? membershipService.resolveDefaultCommunityId(authenticatedUser)
                            : (authenticatedUser.getCommunity() != null ? authenticatedUser.getCommunity().getId() : null);
                }

                // 2. Authorize requested property/flat if specified
                if (requestedPropertyId != null) {
                    if (membershipService != null && !membershipService.isUserAuthorizedForProperty(authenticatedUser, requestedPropertyId)) {
                        log.warn("Access denied: user {} is not authorized for requested property {}",
                                authenticatedUser.getId(), requestedPropertyId);
                        sendForbidden(request, response, "PROPERTY_ACCESS_DENIED",
                                "User is not authorized for the requested property: " + requestedPropertyId);
                        return;
                    }
                    resolvedPropertyId = requestedPropertyId;
                }

                // 3. Resolve organization ID from community and user
                resolvedOrgId = membershipService != null
                        ? membershipService.resolveOrganizationId(resolvedCommunityId, authenticatedUser)
                        : (authenticatedUser.getOrganizationId() != null ? authenticatedUser.getOrganizationId() : TenantContext.DEFAULT_ORG_ID);

            } else {
                // Unauthenticated request (public endpoints, webhooks, registration lookup)
                Long orgHeader = parseLongHeader(request, ORG_HEADER);
                resolvedOrgId = orgHeader != null ? orgHeader : TenantContext.DEFAULT_ORG_ID;
                resolvedCommunityId = requestedCommunityId;
            }

            TenantContext ctx = TenantContext.builder()
                    .organizationId(resolvedOrgId != null ? resolvedOrgId : TenantContext.DEFAULT_ORG_ID)
                    .communityId(resolvedCommunityId)
                    .userId(resolvedUserId)
                    .propertyId(resolvedPropertyId)
                    .build();
            TenantContext.setContext(ctx);

            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
        }
    }

    private AppUser resolveAuthenticatedUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof UserPrincipal up) {
            return up.getUser();
        }
        if (principal instanceof AppUser user) {
            return user;
        }
        if (principal instanceof String principalStr && userRepository != null) {
            try {
                Long userId = Long.parseLong(principalStr);
                return userRepository.findById(userId).orElse(null);
            } catch (NumberFormatException e) {
                return userRepository.findByEmail(principalStr).orElse(null);
            }
        }
        return null;
    }

    private void sendForbidden(HttpServletRequest request, HttpServletResponse response,
                               String errorCode, String message) throws IOException {
        if (securityErrorWriter != null) {
            securityErrorWriter.write(request, response, HttpStatus.FORBIDDEN, errorCode, message);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, message);
        }
    }

    private Long parseLongHeader(HttpServletRequest request, String headerName) {
        String val = request.getHeader(headerName);
        if (val != null && !val.isBlank()) {
            try {
                return Long.parseLong(val.trim());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
