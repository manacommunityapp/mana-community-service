package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Lightweight user summary returned inside a role's user listing.
 *
 * Used by GET /api/roles/{roleName}/users (the "Users &amp; Roles" admin page).
 */
@Data
@Builder
public class UserRoleSummary {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String flatNo;
    private String block;
    private String kycStatus;
    private Boolean isActive;
    private Long communityId;

    /** All roles currently assigned to this user (from {@code app_user_roles}). */
    private List<String> roles;

    /** Effective permissions for this user (role templates or user-override rows). */
    private List<String> permissions;
}
