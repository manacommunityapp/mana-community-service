package com.manacommunity.api.rbac.service;

import com.manacommunity.api.platform.tenant.TenantContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Runtime cross-society tenant boundary guard.
 * Ensures request query/path communityId or loaded entity communityId matches the
 * JWT-derived TenantContextHolder whenever a tenant context is bound on the thread.
 */
@Component
public class CrossSocietyTenantGuard {

    public void assertMatchesActiveTenant(Long targetCommunityId) {
        Long activeTenant = TenantContextHolder.get();
        if (activeTenant != null && targetCommunityId != null && !activeTenant.equals(targetCommunityId)) {
            throw new AccessDeniedException(
                    "Cross-society data access denied: active community " + activeTenant
                            + " cannot access community " + targetCommunityId);
        }
    }
}
