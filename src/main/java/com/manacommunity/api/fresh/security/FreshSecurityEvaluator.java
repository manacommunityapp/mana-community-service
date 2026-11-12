package com.manacommunity.api.fresh.security;

import com.manacommunity.api.fresh.vendor.model.VendorStaffRole;
import com.manacommunity.api.fresh.vendor.repository.FreshVendorStaffRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Spring Security evaluator bean used in `@PreAuthorize` SpEL expressions
 * to enforce multi-tenant vendor data isolation and role boundaries.
 */
@Slf4j
@Component("freshSecurity")
@RequiredArgsConstructor
public class FreshSecurityEvaluator {

    private final FreshVendorStaffRepository staffRepository;

    /**
     * Checks if current user is active staff of the vendor, or has ROLE_ADMIN / ROLE_SUPER_ADMIN.
     */
    public boolean isVendorStaff(UserPrincipal principal, Long vendorId) {
        if (principal == null || vendorId == null) {
            return false;
        }
        if (isAdmin(principal)) {
            return true;
        }
        return staffRepository.existsByVendorIdAndUserIdAndIsActiveTrue(vendorId, principal.getId());
    }

    /**
     * Checks if current user has any of the specified VendorStaffRoles for this vendor, or has ROLE_ADMIN.
     */
    public boolean hasVendorRole(UserPrincipal principal, Long vendorId, String... roleNames) {
        if (principal == null || vendorId == null || roleNames == null || roleNames.length == 0) {
            return false;
        }
        if (isAdmin(principal)) {
            return true;
        }
        for (String roleName : roleNames) {
            try {
                VendorStaffRole role = VendorStaffRole.valueOf(roleName);
                if (staffRepository.existsByVendorIdAndUserIdAndVendorRoleAndIsActiveTrue(vendorId, principal.getId(), role)) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
                log.warn("Unknown VendorStaffRole requested: {}", roleName);
            }
        }
        return false;
    }

    private boolean isAdmin(UserPrincipal principal) {
        return principal.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) ||
                "ROLE_SUPER_ADMIN".equals(a.getAuthority()) ||
                "ROLE_COMMUNITY_ADMIN".equals(a.getAuthority()));
    }
}
