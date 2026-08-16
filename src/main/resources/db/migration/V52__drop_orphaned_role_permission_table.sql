-- Drop the orphaned 'role_permission' (singular) table if it exists.
-- The application uses 'role_permissions' (plural) exclusively via the RolePermission entity.
DROP TABLE IF EXISTS manacommunity.role_permission;
