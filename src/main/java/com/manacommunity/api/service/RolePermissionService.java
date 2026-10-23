package com.manacommunity.api.service;

import com.manacommunity.api.dto.RoleDetailsResponse;
import com.manacommunity.api.user.model.AppUser;

import java.util.List;
import java.util.Map;

public interface RolePermissionService {

    /** Returns all role → permission keys, filtered to the given communityId (null = global). */
    Map<String, List<String>> getAllRolePermissions(Long communityId);

    /**
     * Returns all roles with their permission lists as structured DTOs,
     * scoped to the given community (null = global roles only).
     * Excludes system-reserved roles (SUPER_ADMIN, COMMUNITY_ADMIN).
     * Suitable for the "Access &amp; Roles" admin UI page.
     */
    List<RoleDetailsResponse> getRoleDetails(Long communityId);

    /**
     * Overwrites the role-level permissions for a community-scoped role.
     * Pass communityId = null for global (SUPER_ADMIN) roles.
     */
    void updateRolePermissions(String roleName, Long communityId, List<String> permissions);

    /** Overwrites user-specific permission overrides for the given user.
     *  callerCommunityId is used to verify the target user belongs to the caller's community. */
    void updateUserPermissions(Long userId, String role, List<String> permissions, Long callerCommunityId);

    /** Returns the user-specific permission keys for a given user. */
    List<String> getUserPermissions(Long userId);

    /** Returns the effective permission keys for a user, properly scoped to their community. */
    List<String> getEffectivePermissionsForUser(AppUser user);
}
