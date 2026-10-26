package com.manacommunity.api.insurance.service;

import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.http.HttpStatus;

import java.util.Set;

/** Server-side role gate for insurance administration. Object-level checks live in the services. */
public final class InsuranceAccess {
    private static final Set<String> ADMIN_ROLES = Set.of("SUPER_ADMIN", "ADMIN");

    private InsuranceAccess() {}

    public static boolean isAdmin(AppUser u) {
        return u != null && u.getRole() != null && ADMIN_ROLES.contains(u.getRole().toUpperCase());
    }

    public static void requireAdmin(AppUser u) {
        if (!isAdmin(u)) {
            throw new ManaCommunityException("Insufficient privileges", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
    }
}
