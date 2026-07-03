package com.manacommunity.api.user.dto;

/**
 * Response DTO — one row of the role × menu permission matrix.
 */
public record MenuRolePermissionResponse(
    Long id,
    Long roleId,
    String roleName,
    Long menuId,
    String menuKey,
    String menuLabel,
    Boolean canView,
    Boolean canAdd,
    Boolean canUpdate,
    Boolean canDelete
) {}
