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
import jakarta.servlet.http.Cookie;
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

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CommunityMembershipService communityMembershipService;

    public void setCommunityMembershipService(CommunityMembershipService communityMembershipService) {
        this.communityMembershipService = communityMembershipService;
    }

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

        String token = null;
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        } else if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (CookieAuthHelper.ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token != null && !token.isBlank()) {
            if (jwtTokenProvider.validateToken(token) && jwtTokenProvider.isAccessToken(token)) {
                String jti = jwtTokenProvider.getJti(token);
                if (!tokenBlacklistService.isBlacklisted(jti)) {
                    authenticate(jwtTokenProvider.getUserId(token), token, request);
                } else {
                    log.debug("Rejected blacklisted token jti={}", jti);
                }
            } else if (mockAuthEnabled && token.startsWith("mock-token-")) {
                authenticate(parseLegacyId(token), null, request);
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

    private void authenticate(Long userId, String rawJwtToken, HttpServletRequest request) {
        if (userId == null) return;
        AppUser user = userRepository.findById(userId).orElse(null);
        if (user == null || Boolean.FALSE.equals(user.getIsActive())) return;
        if (rawJwtToken != null && user.getTokenInvalidatedBefore() != null
                && jwtTokenProvider.isIssuedBefore(rawJwtToken, user.getTokenInvalidatedBefore())) {
            log.debug("Rejected token issued before password/session invalidation for userId={}", userId);
            return;
        }
        Long tokenCommunityId = rawJwtToken != null ? jwtTokenProvider.getCommunityId(rawJwtToken) : null;
        Long tokenTowerId = rawJwtToken != null ? jwtTokenProvider.getTowerId(rawJwtToken) : null;
        Long tokenFlatId = rawJwtToken != null ? jwtTokenProvider.getFlatId(rawJwtToken) : null;
        setAuthentication(user, request, tokenCommunityId, tokenTowerId, tokenFlatId);
    }

    private void setAuthentication(AppUser user, HttpServletRequest request) {
        setAuthentication(user, request, null, null, null);
    }

    private void setAuthentication(AppUser user, HttpServletRequest request, Long tokenCommunityId) {
        setAuthentication(user, request, tokenCommunityId, null, null);
    }

    private void setAuthentication(AppUser user, HttpServletRequest request,
                                   Long tokenCommunityId, Long tokenTowerId, Long tokenFlatId) {
        Long requestedHeaderCommunityId = parseLongHeader(request, "X-Community-ID", "X-Community-Id");
        Long activeCommunityId = resolveActiveCommunityId(user, requestedHeaderCommunityId, tokenCommunityId);
        Long requestedHeaderFlatId = parseLongHeader(request, "X-Flat-Id", "X-Property-Id");
        Long requestedHeaderTowerId = parseLongHeader(request, "X-Tower-Id", "X-Tower-ID");

        Long activeFlatId = resolveActiveFlatId(user, activeCommunityId, requestedHeaderFlatId, tokenFlatId);
        Long activeTowerId = resolveActiveTowerId(user, activeCommunityId, requestedHeaderTowerId, tokenTowerId, activeFlatId);

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        boolean isSuperAdmin = user.hasRole(ROLE_SUPER_ADMIN);
        List<String> scopedRoles = resolveScopedRoles(user, activeCommunityId);
        for (String roleName : scopedRoles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
            if (ROLE_SUPER_ADMIN.equals(roleName)) {
                isSuperAdmin = true;
            }
        }

        List<String> effectivePerms = rolePermissionService.getEffectivePermissionsForUser(user);

        Set<String> enabledModules = null;
        if (!isSuperAdmin && activeCommunityId != null) {
            enabledModules = resolveEnabledModules(activeCommunityId);
        }

        for (String permKey : effectivePerms) {
            if (isSuperAdmin || isPermissionAllowedByModule(permKey, enabledModules)) {
                authorities.add(new SimpleGrantedAuthority(permKey));
            }
        }

        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getEmail(), user.getPasswordHash(),
                authorities, user, activeCommunityId, activeTowerId, activeFlatId);

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        org.slf4j.MDC.put(CorrelationIdFilter.MDC_USER_ID, String.valueOf(user.getId()));
        if (activeCommunityId != null) {
            org.slf4j.MDC.put(CorrelationIdFilter.MDC_COMMUNITY_ID, String.valueOf(activeCommunityId));
        }
        if (activeTowerId != null) {
            org.slf4j.MDC.put(CorrelationIdFilter.MDC_TOWER_ID, String.valueOf(activeTowerId));
        }
        if (activeFlatId != null) {
            org.slf4j.MDC.put(CorrelationIdFilter.MDC_FLAT_ID, String.valueOf(activeFlatId));
        }
    }

    private Long parseLongHeader(HttpServletRequest request, String... headerNames) {
        if (request == null || headerNames == null) return null;
        for (String name : headerNames) {
            String headerVal = request.getHeader(name);
            if (headerVal != null && !headerVal.isBlank()) {
                try {
                    return Long.parseLong(headerVal.trim());
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    private Long resolveActiveFlatId(AppUser user, Long activeCommunityId, Long headerFlatId, Long tokenFlatId) {
        if (headerFlatId != null && communityMembershipService != null
                && communityMembershipService.isUserAuthorizedForProperty(user, headerFlatId)) {
            return headerFlatId;
        }
        if (tokenFlatId != null && (communityMembershipService == null
                || communityMembershipService.isUserAuthorizedForProperty(user, tokenFlatId))) {
            return tokenFlatId;
        }
        if (communityMembershipService != null && activeCommunityId != null) {
            var unit = communityMembershipService.resolveDefaultUnitContext(user, activeCommunityId);
            if (unit != null) {
                return unit.flatId();
            }
        }
        return null;
    }

    private Long resolveActiveTowerId(AppUser user, Long activeCommunityId,
                                      Long headerTowerId, Long tokenTowerId, Long activeFlatId) {
        if (headerTowerId != null && communityMembershipService != null
                && communityMembershipService.isUserAuthorizedForTower(user, activeCommunityId, headerTowerId)) {
            return headerTowerId;
        }
        if (tokenTowerId != null) {
            return tokenTowerId;
        }
        if (communityMembershipService != null && activeCommunityId != null) {
            var unit = activeFlatId != null
                    ? communityMembershipService.resolveUnitContextForFlat(user, activeCommunityId, activeFlatId)
                    : null;
            if (unit == null) {
                unit = communityMembershipService.resolveDefaultUnitContext(user, activeCommunityId);
            }
            if (unit != null && unit.towerId() != null) {
                return unit.towerId();
            }
        }
        if (activeCommunityId != null) {
            String fallbackTower = (user.getTower() != null && !user.getTower().isBlank())
                    ? user.getTower()
                    : user.getBlock();
            return CommunityMembershipService.computeTowerId(activeCommunityId, fallbackTower);
        }
        return null;
    }

    private Long resolveActiveCommunityId(AppUser user, Long headerCommunityId, Long tokenCommunityId) {
        if (headerCommunityId != null && communityMembershipService != null
                && communityMembershipService.isUserAuthorizedForCommunity(user, headerCommunityId)) {
            return headerCommunityId;
        }
        if (tokenCommunityId != null && (communityMembershipService == null
                || communityMembershipService.isUserAuthorizedForCommunity(user, tokenCommunityId))) {
            return tokenCommunityId;
        }
        return user.getCommunity() != null ? user.getCommunity().getId() : null;
    }

    private List<String> resolveScopedRoles(AppUser user, Long activeCommunityId) {
        if (communityMembershipService != null && activeCommunityId != null) {
            List<String> scoped = communityMembershipService.resolveRolesForCommunity(user, activeCommunityId);
            if (!scoped.isEmpty()) {
                return scoped;
            }
        }
        List<String> fallback = new ArrayList<>();
        if (user.getRole() != null) {
            for (String r : user.getRole().split(",")) {
                String trimmed = r.trim().toUpperCase();
                if (!trimmed.isEmpty()) {
                    fallback.add(trimmed);
                }
            }
        }
        return fallback;
    }

    private boolean isPermissionAllowedByModule(String permissionKey, Set<String> enabledModules) {
        if (enabledModules == null) {
            return true;
        }
        String moduleKey = ModuleConstants.getModuleForPermission(permissionKey);
        return moduleKey == null || enabledModules.contains(moduleKey);
    }

    private Set<String> resolveEnabledModules(Long communityId) {
        String cacheKey = "community:modules:" + communityId;
        if (redisTemplate != null) {
            try {
                Set<String> cached = redisTemplate.opsForSet().members(cacheKey);
                if (cached != null && !cached.isEmpty()) {
                    return cached;
                }
            } catch (Exception e) {
                log.debug("Redis read failed for key {}: {}", cacheKey, e.getMessage());
            }
        }

        Set<String> modules = communityModuleRepository.findEnabledByCommunityId(communityId)
                .stream()
                .map(com.manacommunity.api.model.CommunityModule::getModuleKey)
                .collect(Collectors.toSet());

        if (redisTemplate != null && !modules.isEmpty()) {
            try {
                redisTemplate.opsForSet().add(cacheKey, modules.toArray(new String[0]));
                redisTemplate.expire(cacheKey, java.time.Duration.ofMinutes(30));
            } catch (Exception e) {
                log.debug("Redis write failed for key {}: {}", cacheKey, e.getMessage());
            }
        }
        return modules;
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
