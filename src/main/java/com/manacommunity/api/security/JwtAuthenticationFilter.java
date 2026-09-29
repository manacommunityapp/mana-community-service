package com.manacommunity.api.security;

import com.manacommunity.api.constants.ModuleConstants;
import com.manacommunity.api.user.security.UserPrincipal;

import static com.manacommunity.api.constants.PermissionConstants.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.RolePermission;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.repository.CommunityModuleRepository;
import com.manacommunity.api.repository.RolePermissionRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AppUserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final CommunityModuleRepository communityModuleRepository;
    private final Environment environment;
    private final TokenBlacklistService tokenBlacklistService;
    private final com.manacommunity.api.service.RolePermissionService rolePermissionService;

    @Value("${app.security.mock-auth-enabled:false}")
    private boolean mockAuthEnabled;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   AppUserRepository userRepository,
                                   RolePermissionRepository rolePermissionRepository,
                                   CommunityModuleRepository communityModuleRepository,
                                   Environment environment,
                                   TokenBlacklistService tokenBlacklistService,
                                   com.manacommunity.api.service.RolePermissionService rolePermissionService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.communityModuleRepository = communityModuleRepository;
        this.environment = environment;
        this.tokenBlacklistService = tokenBlacklistService;
        this.rolePermissionService = rolePermissionService;
    }

    @PostConstruct
    void validateMockAuthProfile() {
        if (!mockAuthEnabled) return;
        boolean isLocalProfile = Arrays.asList(environment.getActiveProfiles()).contains("local");
        if (!isLocalProfile) {
            throw new IllegalStateException(
                    "mock-auth-enabled=true is only allowed with the 'local' profile. "
                    + "Active profiles: " + Arrays.toString(environment.getActiveProfiles())
                    + ". Remove the flag or switch to the local profile.");
        }
        log.warn("*** MOCK AUTH ENABLED — all unauthenticated requests will be auto-elevated. "
               + "This must NEVER reach a deployed environment. ***");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();

            if (jwtTokenProvider.validateToken(token) && jwtTokenProvider.isAccessToken(token)) {
                String jti = jwtTokenProvider.getJti(token);
                if (!tokenBlacklistService.isBlacklisted(jti)) {
                    authenticate(jwtTokenProvider.getUserId(token), request);
                } else {
                    log.debug("Rejected blacklisted token jti={}", jti);
                }
            } else if (mockAuthEnabled && token.startsWith("mock-token-")) {
                authenticate(parseLegacyId(token), request);
            }
        } else if (mockAuthEnabled && !isDocsPath(request)) {
            authenticateDefaultUser(request);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isDocsPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-resources")
                || uri.startsWith("/webjars");
    }

    private void authenticate(Long userId, HttpServletRequest request) {
        if (userId == null) return;
        AppUser user = userRepository.findById(userId).orElse(null);
        if (user == null || Boolean.FALSE.equals(user.getIsActive())) return;
        setAuthentication(user, request);
    }

    private void setAuthentication(AppUser user, HttpServletRequest request) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        boolean isSuperAdmin = false;
        if (user.getRole() != null) {
            String[] roles = user.getRole().split(",");
            for (String r : roles) {
                String trimmed = r.trim().toUpperCase();
                if (!trimmed.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + trimmed));
                    if (ROLE_SUPER_ADMIN.equals(trimmed)) {
                        isSuperAdmin = true;
                    }
                }
            }
        }

        List<String> effectivePerms = rolePermissionService.getEffectivePermissionsForUser(user);

        Set<String> enabledModules = null;
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (!isSuperAdmin && communityId != null) {
            enabledModules = communityModuleRepository.findEnabledByCommunityId(communityId)
                    .stream()
                    .map(cm -> cm.getModuleKey())
                    .collect(Collectors.toSet());
        }

        for (String permKey : effectivePerms) {
            if (isSuperAdmin || isPermissionAllowedByModule(permKey, enabledModules)) {
                authorities.add(new SimpleGrantedAuthority(permKey));
            }
        }

        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getEmail(), user.getPasswordHash(), authorities, user);

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        org.slf4j.MDC.put(CorrelationIdFilter.MDC_USER_ID, String.valueOf(user.getId()));
    }

    private boolean isPermissionAllowedByModule(String permissionKey, Set<String> enabledModules) {
        if (enabledModules == null) {
            return true;
        }
        String moduleKey = ModuleConstants.getModuleForPermission(permissionKey);
        return moduleKey == null || enabledModules.contains(moduleKey);
    }

    private Long parseLegacyId(String token) {
        try {
            return Long.parseLong(token.substring("mock-token-".length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private volatile AppUser cachedDefaultUser;

    private void authenticateDefaultUser(HttpServletRequest request) {
        AppUser user = cachedDefaultUser;
        if (user == null) {
            List<AppUser> allUsers = userRepository.findAll();
            user = allUsers.stream().filter(u -> u.hasRole(ROLE_SUPER_ADMIN)).findFirst()
                    .orElseGet(() -> allUsers.stream().filter(u -> u.hasRole(ROLE_ADMIN)).findFirst()
                    .orElseGet(() -> allUsers.stream().findFirst().orElse(null)));
            cachedDefaultUser = user;
        }
        if (user != null) {
            setAuthentication(user, request);
        }
    }
}
