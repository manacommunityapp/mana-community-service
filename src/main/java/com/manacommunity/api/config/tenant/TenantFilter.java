package com.manacommunity.api.config.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that extracts the tenant identifier from the
 * {@code X-Tenant-ID} HTTP header and stores it in {@link TenantContext}
 * for the duration of the request.
 * <p>
 * If the header is absent, the default schema ({@code manacommunity}) is used,
 * preserving backward compatibility with single-tenant deployments.
 * <p>
 * Runs at {@link Order} {@code 1} so it executes before Spring Security filters
 * and the JPA session is opened with the correct schema.
 */
@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {

    public static final String ORG_HEADER = "X-Organization-Id";
    public static final String COMMUNITY_HEADER = "X-Community-Id";
    public static final String LEGACY_TENANT_HEADER = "X-Tenant-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        Long orgId = parseLongHeader(request, ORG_HEADER);
        Long communityId = parseLongHeader(request, COMMUNITY_HEADER);

        String legacyTenant = request.getHeader(LEGACY_TENANT_HEADER);
        if (communityId == null && legacyTenant != null && !legacyTenant.isBlank()) {
            TenantContext.setTenantId(legacyTenant.trim().toLowerCase());
        }

        TenantContext ctx = TenantContext.builder()
                .organizationId(orgId != null ? orgId : TenantContext.DEFAULT_ORG_ID)
                .communityId(communityId != null ? communityId : TenantContext.getCommunityId())
                .build();
        TenantContext.setContext(ctx);

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
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
