package com.manacommunity.api.service.impl;

import com.manacommunity.api.security.AuditAction;

import com.manacommunity.api.security.AuditModule;

import com.manacommunity.api.security.AuditService;

import com.manacommunity.api.dto.RoleDetailsResponse;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.Role;
import com.manacommunity.api.model.RolePermission;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.repository.RolePermissionRepository;
import com.manacommunity.api.repository.RoleRepository;
import com.manacommunity.api.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolePermissionServiceImpl implements RolePermissionService {

    private final RoleRepository roleRepo;
    private final RolePermissionRepository rolePermissionRepo;
    private final AppUserRepository appUserRepo;
    private final com.manacommunity.api.security.AuditService auditService;


    @Override
    @Transactional(readOnly = true)
    public List<RoleDetailsResponse> getRoleDetails(Long communityId) {
        List<Role> allRoles = roleRepo.findAll().stream()
                .filter(role -> role.getName() != null && !isSystemReserved(role.getName()))
                .filter(role -> {
                    if (communityId != null) {
                        return communityId.equals(role.getCommunityId())
                                || role.getCommunityId() == null;
                    }
                    return true;
                })
                .toList();

        // Separate into community-specific roles and global roles
        java.util.Map<String, Role> communityRoles = allRoles.stream()
                .filter(r -> communityId != null && communityId.equals(r.getCommunityId()))
                .collect(Collectors.toMap(r -> r.getName().toUpperCase(), r -> r, (a, b) -> a));

        java.util.Map<String, Role> globalRoles = allRoles.stream()
                .filter(r -> r.getCommunityId() == null)
                .collect(Collectors.toMap(r -> r.getName().toUpperCase(), r -> r, (a, b) -> a));

        java.util.Set<String> allRoleNames = new java.util.LinkedHashSet<>();
        allRoleNames.addAll(communityRoles.keySet());
        allRoleNames.addAll(globalRoles.keySet());

        return allRoleNames.stream().map(roleName -> {
            Role role = communityRoles.containsKey(roleName)
                    ? communityRoles.get(roleName)
                    : globalRoles.get(roleName);

            List<String> perms = role.getPermissions().stream()
                    .filter(rp -> rp.getUser() == null)
                    .map(RolePermission::getPermissionKey)
                    .distinct()
                    .sorted()
                    .toList();

            long userCount = communityId != null
                    ? appUserRepo.findByCommunityIdAndUserRoleName(communityId, role.getName()).size()
                    : appUserRepo.findByUserRoleName(role.getName()).size();

            return RoleDetailsResponse.builder()
                    .id(role.getId())
                    .name(role.getName())
                    .communityId(role.getCommunityId())
                    .permissions(perms)
                    .userCount(userCount)
                    .build();
        }).toList();
    }

    private boolean isSystemReserved(String roleName) {
        if (roleName == null) return false;
        String upper = roleName.trim().toUpperCase();
        return upper.equals("SUPER_ADMIN") || upper.equals("SUPERADMIN") || upper.equals("SUPER_ADMINISTRATOR")
                || upper.equals("COMMUNITY_ADMIN") || upper.equals("COMMUNITYADMIN")
                || upper.equals("COMMUNITY_ADMINISTRATOR") || upper.equals("COMMUNITY ADMIN");
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<String>> getAllRolePermissions(Long communityId) {
        // Collect ALL role-permission rows for non-reserved roles
        List<RolePermission> allRows = rolePermissionRepo.findAll().stream()
                .filter(rp -> rp.getUser() == null)
                .filter(rp -> {
                    String rName = rp.getRole() != null ? rp.getRole().trim().toUpperCase() : "";
                    if ("SUPER_ADMIN".equals(rName) || "SUPERADMIN".equals(rName) || "SUPER_ADMINISTRATOR".equals(rName)
                            || "COMMUNITY_ADMIN".equals(rName) || "COMMUNITYADMIN".equals(rName)
                            || "COMMUNITY_ADMINISTRATOR".equals(rName) || "COMMUNITY ADMIN".equals(rName)) {
                        return false;
                    }
                    return true;
                })
                .toList();

        // Split into community-scoped and global rows
        java.util.Map<String, List<RolePermission>> communityRows = allRows.stream()
                .filter(rp -> {
                    Role re = rp.getRoleEntity();
                    return re != null && communityId != null && communityId.equals(re.getCommunityId());
                })
                .collect(Collectors.groupingBy(rp -> rp.getRole().toUpperCase()));

        java.util.Map<String, List<RolePermission>> globalRows = allRows.stream()
                .filter(rp -> {
                    Role re = rp.getRoleEntity();
                    return re == null || re.getCommunityId() == null;
                })
                .collect(Collectors.groupingBy(rp -> rp.getRole().toUpperCase()));

        // Merge: if a community-scoped entry exists for a role, use it exclusively.
        // Otherwise fall back to the global template rows.
        java.util.Map<String, List<String>> result = new java.util.LinkedHashMap<>();
        java.util.Set<String> allRoleNames = new java.util.LinkedHashSet<>();
        allRoleNames.addAll(communityRows.keySet());
        allRoleNames.addAll(globalRows.keySet());

        for (String roleName : allRoleNames) {
            List<RolePermission> rows = communityRows.containsKey(roleName)
                    ? communityRows.get(roleName)   // prefer community-scoped
                    : globalRows.get(roleName);      // fall back to global
            result.put(roleName, rows.stream()
                    .map(RolePermission::getPermissionKey)
                    .distinct()
                    .toList());
        }
        return result;
    }

    @Override
    @Transactional
    public void updateRolePermissions(String roleName, Long communityId, List<String> permissions) {
        String normalizedRoleName = roleName != null ? roleName.trim().toUpperCase() : "";

        // ── 1. Always resolve/create a COMMUNITY-SCOPED role so we never mutate the
        //       global template that other communities depend on. ─────────────────────
        Role role;
        if (communityId != null) {
            role = roleRepo.findByNameIgnoreCaseAndCommunityId(normalizedRoleName, communityId)
                    .orElseGet(() -> roleRepo.saveAndFlush(Role.builder()
                            .name(normalizedRoleName)
                            .communityId(communityId)
                            .permissions(new java.util.HashSet<>())
                            .build()));
        } else {
            // Super-admin path (communityId == null): update the global template directly.
            role = roleRepo.findByNameIgnoreCaseAndCommunityIdIsNull(normalizedRoleName)
                    .orElseGet(() -> roleRepo.saveAndFlush(Role.builder()
                            .name(normalizedRoleName)
                            .permissions(new java.util.HashSet<>())
                            .build()));
        }

        // ── 2. Remove existing role-level permission rows ─────────────────────────────
        role.getPermissions().removeIf(rp -> rp.getUser() == null);
        roleRepo.saveAndFlush(role);

        // ── 3. Add the updated permissions ────────────────────────────────────────────
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        permissions.stream()
                .filter(p -> p != null && !p.trim().isEmpty())
                .filter(seen::add)
                .forEach(p -> role.getPermissions().add(
                        RolePermission.builder()
                                .role(normalizedRoleName)
                                .roleEntity(role)
                                .permissionKey(p)
                                .build()
                ));

        roleRepo.save(role);

        auditService.record(
                AuditAction.PERMISSION_CHANGED,
                AuditModule.ADMIN,
                "Role", normalizedRoleName,
                null,
                "permissions=" + seen.size() + " (role-level, community=" + communityId + ")");
    }

    @Override
    @Transactional
    public void updateUserPermissions(Long userId, String role, List<String> permissions, Long callerCommunityId) {
        AppUser user = appUserRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Prevent cross-community permission tampering: non-SUPER_ADMIN callers may only
        // modify users in their own community.
        if (callerCommunityId != null) {
            Long targetCommunityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
            if (!callerCommunityId.equals(targetCommunityId)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Cannot modify permissions for a user in a different community.");
            }
        }

        rolePermissionRepo.deleteByUserId(userId);

        // De-duplicate permission keys while preserving encounter order.
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        List<RolePermission> entities = permissions.stream()
                .filter(p -> p != null && !p.trim().isEmpty())
                .filter(seen::add)   // retains only first occurrence of each key
                .map(p -> RolePermission.builder()
                        .role(role.toUpperCase())
                        .roleEntity(user.getRoleEntity())
                        .permissionKey(p)
                        .user(user)
                        .build())
                .toList();

        rolePermissionRepo.saveAll(entities);
        auditService.record(
                com.manacommunity.api.security.AuditAction.PERMISSION_CHANGED,
                com.manacommunity.api.security.AuditModule.ADMIN,
                "AppUser", String.valueOf(userId),
                null,
                "permissions=" + entities.size() + " (user-level override, role=" + role + ")");
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getUserPermissions(Long userId) {
        return rolePermissionRepo.findByUserId(userId).stream()
                .map(RolePermission::getPermissionKey)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getEffectivePermissionsForUser(AppUser user) {
        if (user == null || user.getId() == null) {
            return java.util.Collections.emptyList();
        }

        // 1. User-specific override rows take highest priority
        List<RolePermission> userOverrides = rolePermissionRepo.findByUserId(user.getId());
        if (!userOverrides.isEmpty()) {
            return userOverrides.stream()
                    .map(RolePermission::getPermissionKey)
                    .distinct()
                    .toList();
        }

        // 2. Resolve community-scoped permissions for the user's roles
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        Map<String, List<String>> activeRolePerms = getAllRolePermissions(communityId);

        java.util.Set<String> effective = new java.util.LinkedHashSet<>();

        // Check structured userRoles first
        if (user.getUserRoles() != null && !user.getUserRoles().isEmpty()) {
            for (Role r : user.getUserRoles()) {
                if (r != null && r.getName() != null) {
                    List<String> rolePerms = activeRolePerms.get(r.getName().toUpperCase());
                    if (rolePerms != null) {
                        effective.addAll(rolePerms);
                    }
                }
            }
        }

        // Fall back to / combine with legacy comma-separated role string
        if (user.getRole() != null && !user.getRole().isBlank()) {
            for (String rStr : user.getRole().split(",")) {
                String trimmed = rStr.trim().toUpperCase();
                if (!trimmed.isEmpty()) {
                    List<String> rolePerms = activeRolePerms.get(trimmed);
                    if (rolePerms != null) {
                        effective.addAll(rolePerms);
                    }
                }
            }
        }

        return java.util.List.copyOf(effective);
    }
}
