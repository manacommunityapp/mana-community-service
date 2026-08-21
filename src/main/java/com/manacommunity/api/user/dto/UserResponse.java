package com.manacommunity.api.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String kycStatus;
    private String profilePicUrl;
    private String gender;
    private LocalDate dateOfBirth;
    private String flatNo;
    private String block;
    private Long communityId;
    private Long roleId;
    private Boolean isActive;
    private java.util.List<String> permissions;
    private java.util.List<String> enabledModules;
    /** Viewable menu permissions for the user's role (menuKey → CRUD flags). */
    private java.util.List<MenuRolePermissionResponse> menuPermissions;
    /** Primary / combined assigned roles list for this user. */
    private java.util.List<String> roles;
    /** When the role was last changed by an admin. */
    private LocalDateTime roleChangedAt;
    /** ID of the admin who last changed this user's role. */
    private Long roleChangedBy;
}
