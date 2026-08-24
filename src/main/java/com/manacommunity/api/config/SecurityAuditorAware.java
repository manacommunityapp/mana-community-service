package com.manacommunity.api.config;

import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Supplies the current authenticated user's ID to Spring Data JPA's
 * @CreatedBy / @LastModifiedBy auditing mechanism.
 *
 * Returns empty when no authentication is present (background jobs,
 * scheduled tasks, Flyway migrations) so those writes are stored with
 * NULL rather than failing.
 */
@Component("auditorAware")
public class SecurityAuditorAware implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        if (auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.ofNullable(principal.getId());
        }
        return Optional.empty();
    }
}
