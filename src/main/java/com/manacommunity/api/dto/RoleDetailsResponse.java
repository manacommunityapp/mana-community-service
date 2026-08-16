package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * UI-friendly projection for a role and its permission keys.
 *
 * Used by:
 *  - GET /api/roles/details         → "Access &amp; Roles" admin page
 *  - GET /api/roles/{role}/users    → "Users &amp; Roles" admin page (role header)
 */
@Data
@Builder
public class RoleDetailsResponse {

    /** Database PK of the role entity. */
    private Long id;

    /** Normalised role name, e.g. "ADMIN", "SPORTS_ADMIN", "USER". */
    private String name;

    /**
     * Community this role is scoped to.
     * {@code null} means it is a global (SUPER_ADMIN-level) role.
     */
    private Long communityId;

    /**
     * All permission keys currently stored for this role in the
     * {@code role_permissions} table (no user-override rows).
     */
    private List<String> permissions;

    /**
     * Number of community members currently assigned this role
     * (sourced from the {@code app_user_roles} join table).
     */
    private long userCount;
}
