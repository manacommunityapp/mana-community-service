package com.manacommunity.api.platform.tenant;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Spring Security component for declarative community-access checks.
 * Inject into {@code @PreAuthorize("@tenantGuard.canAccess(authentication, #communityId)")}
 * on controller or service methods.
 */
@Component
public class TenantGuard {

    /**
     * Returns true only if the current request's tenant context matches the
     * given community ID. The context is set from JWT claims by
     * {@link TenantSecurityFilter}, so this cannot be spoofed by a header alone.
     */
    public boolean canAccess(Authentication auth, Long communityId) {
        Long ctx = TenantContextHolder.get();
        return ctx != null && ctx.equals(communityId);
    }
}
