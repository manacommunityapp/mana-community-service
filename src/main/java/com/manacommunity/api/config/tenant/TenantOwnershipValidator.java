package com.manacommunity.api.config.tenant;

import com.manacommunity.api.exception.UnauthorizedActionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Validates entity and repository ownership against the active TenantContext
 * to protect against Insecure Direct Object Reference (IDOR) attacks.
 */
@Component
public class TenantOwnershipValidator {

    /**
     * Asserts that the target resource belongs to the currently resolved community.
     * Throws AccessDeniedException if there is a mismatch.
     */
    public void validateCommunityOwnership(Long resourceCommunityId) {
        if (resourceCommunityId == null) {
            return;
        }

        Long activeCommunityId = TenantContext.getCommunityId();
        if (activeCommunityId != null && !activeCommunityId.equals(resourceCommunityId)) {
            throw new AccessDeniedException(
                    "Cross-tenant access rejected: resource community (" + resourceCommunityId
                            + ") does not match active tenant (" + activeCommunityId + ")");
        }
    }

    /**
     * Asserts that the target resource belongs to the currently resolved organization.
     */
    public void validateOrganizationOwnership(Long resourceOrgId) {
        if (resourceOrgId == null) {
            return;
        }

        Long activeOrgId = TenantContext.getOrganizationId();
        if (activeOrgId != null && !activeOrgId.equals(resourceOrgId)) {
            throw new AccessDeniedException(
                    "Cross-organization access rejected: resource organization (" + resourceOrgId
                            + ") does not match active organization (" + activeOrgId + ")");
        }
    }

    /**
     * Returns true if the resource community matches the active TenantContext or if active context is global.
     */
    public boolean matchesCurrentCommunity(Long resourceCommunityId) {
        if (resourceCommunityId == null) return true;
        Long active = TenantContext.getCommunityId();
        return active == null || active.equals(resourceCommunityId);
    }
}
