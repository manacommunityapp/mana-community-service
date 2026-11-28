package com.manacommunity.api.module;

import com.manacommunity.api.service.CommunityModuleService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Enforces @ModuleRequired before any annotated controller method executes.
 *
 * The aspect reads the community ID from the currently-authenticated
 * UserPrincipal (via LoggedInUserService) and delegates to
 * CommunityModuleService.assertEnabled, which throws
 * ModuleNotEnabledException (→ HTTP 403) when the module is disabled.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ModuleRequiredAspect {

    private final CommunityModuleService moduleService;
    private final LoggedInUserService loggedInUserService;

    @Before("@annotation(moduleRequired)")
    public void checkModule(JoinPoint joinPoint, ModuleRequired moduleRequired) {
        String moduleKey = moduleRequired.value();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            // Unauthenticated calls are handled by the security filter chain.
            // Don't block here — let Spring Security send the 401.
            return;
        }

        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();

        log.debug("Module guard: checking '{}' for community {}", moduleKey, communityId);
        moduleService.assertEnabled(communityId, moduleKey);
    }
}
