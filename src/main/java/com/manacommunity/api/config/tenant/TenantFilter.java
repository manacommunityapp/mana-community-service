package com.manacommunity.api.config.tenant;

import com.manacommunity.api.security.CommunityMembershipService;
import com.manacommunity.api.security.CorrelationIdFilter;
import com.manacommunity.api.security.SecurityErrorWriter;
import org.slf4j.MDC;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
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
@Order(1)
@Component
public class TenantFilter extends OncePerRequestFilter {

    public static final String ORG_HEADER = "X-Organization-Id";
    public static final String COMMUNITY_HEADER = "X-Community-Id";
    public static final String LEGACY_TENANT_HEADER = "X-Tenant-ID";
    public static final String TOWER_HEADER = "X-Tower-Id";
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

            Long requestedTowerId = parseLongHeader(request, TOWER_HEADER);
            Long requestedPropertyId = parseLongHeader(request, PROPERTY_HEADER);
            if (requestedPropertyId == null) {
                requestedPropertyId = parseLongHeader(request, FLAT_HEADER);
            }

            Long resolvedCommunityId = null;
            Long resolvedOrgId = null;
            Long resolvedUserId = null;
            Long resolvedPropertyId = null;
            Long resolvedTowerId = null;
            String resolvedTowerBlock = null;

            if (authenticatedUser != null) {
                resolvedUserId = authenticatedUser.getId();
                UserPrincipal userPrincipal = auth.getPrincipal() instanceof UserPrincipal up ? up : null;
                boolean isSuperAdmin = auth.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));

                // 1. Authorize requested community against server-side memberships
                if (requestedCommunityId != null) {
                    if (membershipService != null) {
                        if (!membershipService.isUserAuthorizedForCommunity(authenticatedUser, requestedCommunityId)) {
                            log.warn("Access denied: user {} is not authorized for requested community {}",
                                    authenticatedUser.getId(), requestedCommunityId);
                            sendForbidden(request, response, "TENANT_ACCESS_DENIED",
                                    "User is not authorized for the requested community: " + requestedCommunityId);
                            return;
                        }
                        resolvedCommunityId = requestedCommunityId;
                    } else if (isSuperAdmin) {
                        resolvedCommunityId = requestedCommunityId;
                    } else {
                        resolvedCommunityId = authenticatedUser.getCommunity() != null
                                ? authenticatedUser.getCommunity().getId()
                                : (userPrincipal != null ? userPrincipal.getCommunityId() : null);
                        if (resolvedCommunityId != null && !resolvedCommunityId.equals(requestedCommunityId)) {
                            log.warn("Discarded unauthorized community header {} for user {} (principal community={})",
                                    requestedCommunityId, authenticatedUser.getId(), resolvedCommunityId);
                        }
                    }
                } else {
                    // Default to user's assigned community
                    resolvedCommunityId = membershipService != null
                            ? membershipService.resolveDefaultCommunityId(authenticatedUser)
                            : (authenticatedUser.getCommunity() != null ? authenticatedUser.getCommunity().getId() : null);
                }

                // 2. Authorize requested property/flat if specified, else resolve active flat from principal/membership
                if (requestedPropertyId != null) {
                    if (membershipService != null && !membershipService.isUserAuthorizedForProperty(authenticatedUser, requestedPropertyId)) {
                        log.warn("Access denied: user {} is not authorized for requested property {}",
                                authenticatedUser.getId(), requestedPropertyId);
                        sendForbidden(request, response, "PROPERTY_ACCESS_DENIED",
                                "User is not authorized for the requested property: " + requestedPropertyId);
                        return;
                    }
                    resolvedPropertyId = requestedPropertyId;
                } else if (userPrincipal != null && userPrincipal.getFlatId() != null) {
                    resolvedPropertyId = userPrincipal.getFlatId();
                }

                // 3. Resolve & authorize tower context (header > unit lookup > principal > profile)
                if (requestedTowerId != null) {
                    if (membershipService != null
                            && !membershipService.isUserAuthorizedForTower(authenticatedUser, resolvedCommunityId, requestedTowerId)) {
                        log.warn("Access denied: user {} is not authorized for requested tower {} in community {}",
                                authenticatedUser.getId(), requestedTowerId, resolvedCommunityId);
                        sendForbidden(request, response, "TOWER_ACCESS_DENIED",
                                "User is not authorized for the requested tower: " + requestedTowerId);
                        return;
                    }
                    resolvedTowerId = requestedTowerId;
                } else if (userPrincipal != null && userPrincipal.getTowerId() != null) {
                    resolvedTowerId = userPrincipal.getTowerId();
                }

                if (membershipService != null && resolvedCommunityId != null) {
                    var unit = resolvedPropertyId != null
                            ? membershipService.resolveUnitContextForFlat(authenticatedUser, resolvedCommunityId, resolvedPropertyId)
                            : membershipService.resolveDefaultUnitContext(authenticatedUser, resolvedCommunityId);
                    if (unit != null) {
                        if (resolvedPropertyId == null) resolvedPropertyId = unit.flatId();
                        if (resolvedTowerId == null) resolvedTowerId = unit.towerId();
                        resolvedTowerBlock = unit.towerBlock();
                    }
                }
                if (resolvedTowerId == null && resolvedCommunityId != null) {
                    String profileTower = (authenticatedUser.getTower() != null && !authenticatedUser.getTower().isBlank())
                            ? authenticatedUser.getTower()
                            : authenticatedUser.getBlock();
                    resolvedTowerBlock = profileTower;
                    resolvedTowerId = CommunityMembershipService.computeTowerId(resolvedCommunityId, profileTower);
                }

                // 4. Resolve organization ID from community and user
                resolvedOrgId = membershipService != null
                        ? membershipService.resolveOrganizationId(resolvedCommunityId, authenticatedUser)
                        : (authenticatedUser.getOrganizationId() != null ? authenticatedUser.getOrganizationId() : TenantContext.DEFAULT_ORG_ID);

            } else {
                // Unauthenticated request (public endpoints, webhooks, registration lookup)
                Long orgHeader = parseLongHeader(request, ORG_HEADER);
                resolvedOrgId = orgHeader != null ? orgHeader : TenantContext.DEFAULT_ORG_ID;
                resolvedCommunityId = null;
            }

            TenantContext ctx = TenantContext.builder()
                    .organizationId(resolvedOrgId != null ? resolvedOrgId : TenantContext.DEFAULT_ORG_ID)
                    .communityId(resolvedCommunityId)
                    .towerId(resolvedTowerId)
                    .towerBlock(resolvedTowerBlock)
                    .flatId(resolvedPropertyId)
                    .propertyId(resolvedPropertyId)
                    .userId(resolvedUserId)
                    .build();
            TenantContext.setContext(ctx);
            bindMdc(resolvedUserId, resolvedCommunityId, resolvedTowerId, resolvedPropertyId);

            filterChain.doFilter(request, response);

        } finally {
            clearMdc();
            TenantContext.clear();
        }
    }

    private void bindMdc(Long userId, Long communityId, Long towerId, Long flatId) {
        if (userId != null) MDC.put(CorrelationIdFilter.MDC_USER_ID, String.valueOf(userId));
        if (communityId != null) MDC.put(CorrelationIdFilter.MDC_COMMUNITY_ID, String.valueOf(communityId));
        if (towerId != null) MDC.put(CorrelationIdFilter.MDC_TOWER_ID, String.valueOf(towerId));
        if (flatId != null) MDC.put(CorrelationIdFilter.MDC_FLAT_ID, String.valueOf(flatId));
    }

    private void clearMdc() {
        MDC.remove(CorrelationIdFilter.MDC_COMMUNITY_ID);
        MDC.remove(CorrelationIdFilter.MDC_TOWER_ID);
        MDC.remove(CorrelationIdFilter.MDC_FLAT_ID);
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
