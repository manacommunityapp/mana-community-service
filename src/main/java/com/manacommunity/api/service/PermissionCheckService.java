package com.manacommunity.api.service;

import com.manacommunity.api.constants.ModuleConstants;
import com.manacommunity.api.user.service.LoggedInUserService;

import static com.manacommunity.api.constants.PermissionConstants.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.RolePermission;
import com.manacommunity.api.repository.CommunityModuleRepository;
import com.manacommunity.api.repository.RolePermissionRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionCheckService {

    private final RolePermissionRepository rolePermissionRepository;
    private final CommunityModuleRepository communityModuleRepository;
    private final LoggedInUserService loggedInUserService;

    public boolean hasAnyPermission(UserPrincipal principal, String... requiredPermissions) {
        AppUser user = loggedInUserService.resolve(principal);
        if (user.hasRole(ROLE_SUPER_ADMIN)) {
            return true;
        }
        Set<String> userPerms = loadPermissionsFromDB(user);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return Arrays.stream(requiredPermissions)
                .anyMatch(p -> userPerms.contains(p) && isModuleEnabledForPermission(communityId, p));
    }

    public void requireAnyPermission(UserPrincipal principal, String... requiredPermissions) {
        if (!hasAnyPermission(principal, requiredPermissions)) {
            throw new AccessDeniedException("Insufficient permissions. Required any of: "
                    + String.join(", ", requiredPermissions));
        }
    }

    public boolean hasAllPermissions(UserPrincipal principal, String... requiredPermissions) {
        AppUser user = loggedInUserService.resolve(principal);
        if (user.hasRole(ROLE_SUPER_ADMIN)) {
            return true;
        }
        Set<String> userPerms = loadPermissionsFromDB(user);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return Arrays.stream(requiredPermissions)
                .allMatch(p -> userPerms.contains(p) && isModuleEnabledForPermission(communityId, p));
    }

    private boolean isModuleEnabledForPermission(Long communityId, String permissionKey) {
        String moduleKey = ModuleConstants.getModuleForPermission(permissionKey);
        if (moduleKey == null || communityId == null) {
            return true;
        }
        return communityModuleRepository.isModuleEnabled(communityId, moduleKey);
    }

    private Set<String> loadPermissionsFromDB(AppUser user) {
        List<RolePermission> userPerms = rolePermissionRepository.findByUserId(user.getId());
        if (!userPerms.isEmpty()) {
            return userPerms.stream()
                    .map(RolePermission::getPermissionKey)
                    .collect(Collectors.toSet());
        }
        List<RolePermission> rolePerms = rolePermissionRepository.findByRoleIgnoreCase(user.getRole());
        return rolePerms.stream()
                .map(RolePermission::getPermissionKey)
                .collect(Collectors.toSet());
    }
}
