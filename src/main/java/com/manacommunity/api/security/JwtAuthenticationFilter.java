package com.manacommunity.api.security;

import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.model.RolePermission;
import com.manacommunity.api.repository.AppUserRepository;
import com.manacommunity.api.repository.RolePermissionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Production-grade authentication filter. Reads the {@code Authorization: Bearer <jwt>}
 * header, verifies the token's signature / issuer / expiry via {@link JwtTokenProvider},
 * and populates the {@link SecurityContextHolder} with the user's authorities (role +
 * DB-backed permissions). Replaces the old MockJwtFilter.
 *
 * <p>Two developer conveniences remain, both gated by {@code app.security.mock-auth-enabled}
 * (which is {@code false} in production): a legacy {@code mock-token-<id>} token, and a
 * default-user fallback when no token is sent. With the flag off, only a valid signed JWT
 * authenticates a request.</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AppUserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Value("${app.security.mock-auth-enabled:false}")
    private boolean mockAuthEnabled;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   AppUserRepository userRepository,
                                   RolePermissionRepository rolePermissionRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.rolePermissionRepository = rolePermissionRepository;
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
                // Only access tokens authenticate API calls; a refresh token presented as a
                // Bearer credential is rejected (it may only be exchanged at /api/auth/refresh).
                authenticate(jwtTokenProvider.getUserId(token), request);
            } else if (mockAuthEnabled && token.startsWith("mock-token-")) {
                // DEV ONLY — legacy mock token "mock-token-<id>".
                authenticate(parseLegacyId(token), request);
            }
            // An invalid/expired token simply leaves the request unauthenticated;
            // protected endpoints then return 401/403 via Spring Security.
        } else if (mockAuthEnabled && !isDocsPath(request)) {
            // DEV ONLY — no token: authenticate a default admin for convenience.
            // EXCEPT for the API-docs / Swagger UI paths: those are SUPER_ADMIN-only
            // and a tokenless (e.g. plain browser) request must NOT be auto-elevated
            // to the default super-admin, otherwise anyone could open Swagger in dev.
            authenticateDefaultUser(request);
        }

        filterChain.doFilter(request, response);
    }

    /** API-docs / Swagger UI paths — excluded from the dev mock-auth default-user fallback. */
    private boolean isDocsPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-resources")
                || uri.startsWith("/webjars");
    }

    /** Builds the SecurityContext from a resolved user id. */
    private void authenticate(Long userId, HttpServletRequest request) {
        if (userId == null) return;
        AppUser user = userRepository.findById(userId).orElse(null);
        if (user == null || Boolean.FALSE.equals(user.getIsActive())) return;
        setAuthentication(user, request);
    }

    private void setAuthentication(AppUser user, HttpServletRequest request) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));

        // User-specific permissions first, falling back to role-based permissions.
        List<RolePermission> userPerms = rolePermissionRepository.findByUserId(user.getId());
        List<RolePermission> permissions = userPerms.isEmpty()
                ? rolePermissionRepository.findByRoleIgnoreCase(user.getRole())
                : userPerms;
        for (RolePermission perm : permissions) {
            authorities.add(new SimpleGrantedAuthority(perm.getPermissionKey()));
        }

        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getEmail(), user.getPasswordHash(), authorities);

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // Expose the authenticated user id to logs for this request. CorrelationIdFilter
        // owns the MDC lifecycle and clears this key after the request completes.
        org.slf4j.MDC.put(CorrelationIdFilter.MDC_USER_ID, String.valueOf(user.getId()));
    }

    private Long parseLegacyId(String token) {
        try {
            return Long.parseLong(token.substring("mock-token-".length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** DEV ONLY default-user fallback (gated by app.security.mock-auth-enabled). */
    private void authenticateDefaultUser(HttpServletRequest request) {
        AppUser user = userRepository.findAll().stream()
                .filter(u -> "SUPER_ADMIN".equalsIgnoreCase(u.getRole()))
                .findFirst()
                .orElseGet(() -> userRepository.findAll().stream()
                        .filter(u -> "ADMIN".equalsIgnoreCase(u.getRole()))
                        .findFirst()
                        .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null)));
        if (user != null) {
            setAuthentication(user, request);
        }
    }
}
