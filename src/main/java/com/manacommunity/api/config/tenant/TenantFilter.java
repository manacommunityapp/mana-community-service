package com.manacommunity.api.config.tenant;

import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Populates {@link TenantContext} for the duration of the request.
 *
 * <h3>Where the community id comes from</h3>
 * The community is taken from the <strong>authenticated principal</strong>
 * ({@link UserPrincipal#getCommunityId()}), never from a request header. The
 * {@code X-Community-Id} / {@code X-Tenant-ID} headers are client-supplied and were
 * previously trusted verbatim, which let any caller write ledger rows and tenant-scoped
 * entities into another community's books simply by changing a header.
 * <p>
 * The headers are still honoured for {@code ROLE_SUPER_ADMIN}, who legitimately operates
 * across communities. For every other caller a header that disagrees with the principal
 * is logged and discarded.
 *
 * <h3>Filter ordering</h3>
 * Spring Boot registers the Spring Security chain ({@code springSecurityFilterChain}) at
 * order {@code -100} ({@code SecurityProperties.DEFAULT_FILTER_ORDER}). This filter is at
 * {@link Order} {@code 1}, i.e. <em>after</em> it in the servlet chain, so it executes
 * downstream of {@code FilterChainProxy} — by which point
 * {@code JwtAuthenticationFilter} (added inside the security chain via
 * {@code addFilterBefore} in {@code SecurityConfig}, with its global auto-registration
 * explicitly disabled) has already populated the {@link SecurityContextHolder}.
 * The original javadoc claimed the opposite ("runs before Spring Security filters");
 * that was wrong, and it is the reason reading the principal here works.
 */
@Slf4j
@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {

    public static final String ORG_HEADER = "X-Organization-Id";
    public static final String COMMUNITY_HEADER = "X-Community-Id";
    public static final String LEGACY_TENANT_HEADER = "X-Tenant-ID";

    private static final String SUPER_ADMIN_AUTHORITY = "ROLE_SUPER_ADMIN";
    private static final String LEGACY_TENANT_PREFIX = "community_";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // NOTE: the organization header is left as-is — organizationId is not carried on
        // the principal and is not used for data scoping. It must not be used for
        // authorization decisions until it is resolved from the token as well.
        Long orgId = parseLongHeader(request, ORG_HEADER);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long principalCommunityId = null;
        boolean superAdmin = false;
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            if (auth.getPrincipal() instanceof UserPrincipal principal) {
                principalCommunityId = principal.getCommunityId();
            }
            superAdmin = hasSuperAdmin(auth);
        }

        Long requestedCommunityId = parseLongHeader(request, COMMUNITY_HEADER);
        if (requestedCommunityId == null) {
            requestedCommunityId = parseLegacyTenantHeader(request);
        }

        Long communityId = principalCommunityId;
        if (superAdmin && requestedCommunityId != null) {
            // Platform administrators may legitimately act on another community.
            communityId = requestedCommunityId;
        } else if (requestedCommunityId != null && !requestedCommunityId.equals(principalCommunityId)) {
            log.warn("Ignoring client-supplied community id {} on {} {} — authenticated principal "
                            + "belongs to community {}",
                    requestedCommunityId, request.getMethod(), request.getRequestURI(), principalCommunityId);
        }

        TenantContext ctx = TenantContext.builder()
                .organizationId(orgId != null ? orgId : TenantContext.DEFAULT_ORG_ID)
                .communityId(communityId)
                .build();
        TenantContext.setContext(ctx);

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private static boolean hasSuperAdmin(Authentication auth) {
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (SUPER_ADMIN_AUTHORITY.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private Long parseLegacyTenantHeader(HttpServletRequest request) {
        String legacyTenant = request.getHeader(LEGACY_TENANT_HEADER);
        if (legacyTenant == null || legacyTenant.isBlank()) return null;
        String value = legacyTenant.trim().toLowerCase();
        if (!value.startsWith(LEGACY_TENANT_PREFIX)) return null;
        try {
            return Long.parseLong(value.substring(LEGACY_TENANT_PREFIX.length()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long parseLongHeader(HttpServletRequest request, String headerName) {
        String val = request.getHeader(headerName);
        if (val != null && !val.isBlank()) {
            try {
                return Long.parseLong(val.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
