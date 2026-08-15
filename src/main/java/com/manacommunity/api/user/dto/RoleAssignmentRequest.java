package com.manacommunity.api.user.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request body for assigning / upgrading a user's role.
 * Used by: PUT /api/users/{id}/role
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignmentRequest {

    /** The target role name or comma-separated roles. */
    private String role;

    /** Multiple roles list, e.g. ["MEMBER", "SPORTS_ADMIN", "VENDOR"]. */
    private java.util.List<String> roles;

    /** Optional: pin the user to a specific custom Role entity (by id)
     *  instead of the standard role template. Leave null to use the default template. */
    private Long roleId;

    /** Optional free-text reason for the role change — stored in audit log. */
    private String reason;
}
