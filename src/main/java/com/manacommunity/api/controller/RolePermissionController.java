package com.manacommunity.api.controller;

import static com.manacommunity.api.constants.PermissionConstants.*;
import com.manacommunity.api.dto.RoleDetailsResponse;
import com.manacommunity.api.dto.UserRoleSummary;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.Role;
import com.manacommunity.api.model.RolePermission;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.service.RolePermissionService;
import com.manacommunity.api.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;
    private final LoggedInUserService loggedInUserService;
    private final RoleService roleService;
    private final AppUserRepository appUserRepo;

    /**
     * GET /api/roles/permissions
     * Returns all roles mapped to their active permission keys, scoped to the caller's community.
     */
    @GetMapping("/permissions")
    public ResponseEntity<Map<String, List<String>>> getAllRolePermissions(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        return ResponseEntity.ok(rolePermissionService.getAllRolePermissions(communityId));
    }

    /**
     * GET /api/roles/details
     * Returns all roles with their permission lists as structured DTOs,
     * scoped to the caller's community. Designed for the "Access &amp; Roles" admin UI page.
     *
     * Response shape per item:
     * <pre>
     * {
     *   "id": 5,
     *   "name": "SPORTS_ADMIN",
     *   "communityId": 1,
     *   "permissions": ["View Sports", "Manage Sports", ...],
     *   "userCount": 3
     * }
     * </pre>
     */
    @GetMapping("/details")
    public ResponseEntity<List<RoleDetailsResponse>> getRoleDetails(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        return ResponseEntity.ok(rolePermissionService.getRoleDetails(communityId));
    }

    /**
     * GET /api/roles/{roleName}/users
     * Returns all users assigned to the given role within the caller's community.
     * Used by the "Users &amp; Roles" admin UI page to display users per role.
     *
     * Response shape per item:
     * <pre>
     * {
     *   "id": 42,
     *   "fullName": "Alice",
     *   "email": "alice@example.com",
     *   "roles": ["SPORTS_ADMIN", "USER"],
     *   "permissions": [...]
     * }
     * </pre>
     */
    @GetMapping("/{roleName}/users")
    public ResponseEntity<List<UserRoleSummary>> getUsersByRole(
            @PathVariable String roleName,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long communityId = resolveCommunityId(principal);

        List<AppUser> users = (communityId != null)
                ? appUserRepo.findByCommunityIdAndUserRoleName(communityId, roleName)
                : appUserRepo.findByUserRoleName(roleName);

        List<UserRoleSummary> result = users.stream().map(u -> {
            // Derive roles from the authoritative userRoles set
            List<String> roleNames = u.getUserRoles() != null
                    ? u.getUserRoles().stream()
                            .map(com.manacommunity.api.model.Role::getName)
                            .sorted()
                            .toList()
                    : java.util.Collections.emptyList();

            // Derive permissions: user-override rows first, then role templates
            List<String> perms = resolvePermissions(u);

            return UserRoleSummary.builder()
                    .id(u.getId())
                    .fullName(u.getFullName())
                    .email(u.getEmail())
                    .phone(u.getPhone())
                    .flatNo(u.getFlatNo())
                    .block(u.getBlock())
                    .kycStatus(u.getKycStatus())
                    .isActive(u.getIsActive())
                    .communityId(u.getCommunity() != null ? u.getCommunity().getId() : null)
                    .roles(roleNames)
                    .permissions(perms)
                    .build();
        }).toList();

        return ResponseEntity.ok(result);
    }

    /**
     * PUT /api/roles/{role}/permissions
     * Overwrites permissions for a role (no userId) or for a specific user override (userId provided).
     */
    @PutMapping("/{role}/permissions")
    public ResponseEntity<Void> updateRolePermissions(
            @PathVariable String role,
            @RequestBody List<String> permissions,
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long communityId = resolveCommunityId(principal);
        if (userId != null) {
            rolePermissionService.updateUserPermissions(userId, role, permissions, communityId);
        } else {
            rolePermissionService.updateRolePermissions(role, communityId, permissions);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/roles
     * Returns a list of all roles.
     */
    @GetMapping
    public ResponseEntity<List<Role>> getRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    /**
     * POST /api/roles
     * Creates a new role.
     */
    @PostMapping
    public ResponseEntity<?> createRole(@RequestBody Map<String, String> body) {
        String roleName = body.get("name");
        Role saved = roleService.createRole(roleName);
        return ResponseEntity.ok(saved);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Long resolveCommunityId(UserPrincipal principal) {
        if (principal == null) return null;
        try {
            AppUser user = loggedInUserService.resolve(principal);
            if (user.hasRole(ROLE_SUPER_ADMIN)) return null;
            return user.getCommunity() != null ? user.getCommunity().getId() : null;
        } catch (Exception ex) {
            log.warn("Failed to resolve community for principal: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Returns the effective permissions for a user sourced from the role template rows
     * in the {@code role_permissions} table (no user-override rows included here,
     * since those require the full RolePermissionRepository — use the
     * {@code GET /api/users/{id}/permissions} endpoint when user-level overrides matter).
     */
    private List<String> resolvePermissions(AppUser user) {
        if (user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (com.manacommunity.api.model.Role r : user.getUserRoles()) {
            if (r.getPermissions() != null) {
                r.getPermissions().stream()
                        .filter(rp -> rp.getUser() == null)
                        .map(RolePermission::getPermissionKey)
                        .forEach(seen::add);
            }
        }
        return java.util.List.copyOf(seen);
    }
}
