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
    public Map<String, List<String>> getAllRolePermissions(Long communityId) {
        return rolePermissionRepo.findAll().stream()
                .filter(rp -> rp.getUser() == null)
                .filter(rp -> {
                    String rName = rp.getRole() != null ? rp.getRole().trim().toUpperCase() : "";
                    if ("SUPER_ADMIN".equals(rName) || "SUPERADMIN".equals(rName) || "SUPER_ADMINISTRATOR".equals(rName)
                            || "COMMUNITY_ADMIN".equals(rName) || "COMMUNITYADMIN".equals(rName) || "COMMUNITY_ADMINISTRATOR".equals(rName) || "COMMUNITY ADMIN".equals(rName)) {
                        return false;
                    }
                    Role roleEntity = rp.getRoleEntity();
                    if (roleEntity == null) return true;
                    if (communityId != null) {
                        return roleEntity.getCommunityId() == null
                                || roleEntity.getCommunityId().equals(communityId);
                    }
                    return true;
                })
                .collect(Collectors.groupingBy(
                        rp -> rp.getRole().toUpperCase(),
                        Collectors.mapping(RolePermission::getPermissionKey, Collectors.toList())
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleDetailsResponse> getRoleDetails(Long communityId) {
        return roleRepo.findAll().stream()
                .filter(role -> role.getName() != null && !isSystemReserved(role.getName()))
                .filter(role -> {
                    if (communityId != null) {
                        return communityId.equals(role.getCommunityId())
                                || role.getCommunityId() == null;
                    }
                    return true;
                })
                .map(role -> {
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
                })
                .toList();
    }

    private boolean isSystemReserved(String roleName) {
        if (roleName == null) return false;
        String upper = roleName.trim().toUpperCase();
        return upper.equals("SUPER_ADMIN") || upper.equals("SUPERADMIN") || upper.equals("SUPER_ADMINISTRATOR")
                || upper.equals("COMMUNITY_ADMIN") || upper.equals("COMMUNITYADMIN")
                || upper.equals("COMMUNITY_ADMINISTRATOR") || upper.equals("COMMUNITY ADMIN");
    }

    @Override
    @Transactional
    public void updateRolePermissions(String roleName, Long communityId, List<String> permissions) {
        String normalizedRoleName = roleName != null ? roleName.trim().toUpperCase() : "";

        // ── 1. Resolve the Role entity (community-scoped first, then global fallback) ──
        Role role;
        if (communityId != null) {
            role = roleRepo.findByNameIgnoreCaseAndCommunityId(normalizedRoleName, communityId)
                    .orElseGet(() -> roleRepo.findByNameIgnoreCaseAndCommunityIdIsNull(normalizedRoleName)
                            .orElseGet(() -> roleRepo.saveAndFlush(Role.builder()
                                    .name(normalizedRoleName)
                                    .communityId(communityId)
                                    .permissions(new java.util.HashSet<>())
                                    .build())));
        } else {
            role = roleRepo.findByNameIgnoreCaseAndCommunityIdIsNull(normalizedRoleName)
                    .orElseGet(() -> roleRepo.saveAndFlush(Role.builder()
                            .name(normalizedRoleName)
                            .permissions(new java.util.HashSet<>())
                            .build()));
        }

        // ── 2. Remove existing role-level permission rows via the collection ─────────
        // Using the collection (not a bulk JPQL DELETE) keeps the role entity managed
        // throughout the transaction.  orphanRemoval=true on the OneToMany ensures
        // the removed RolePermission rows are DELETEd from the DB on flush.
        role.getPermissions().removeIf(rp -> rp.getUser() == null);

        // Flush the DELETEs first so the unique constraint
        // (role_id, permission_key, user_id) is not violated by the upcoming INSERTs.
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
    public void updateUserPermissions(Long userId, String role, List<String> permissions) {
        AppUser user = appUserRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

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
}
