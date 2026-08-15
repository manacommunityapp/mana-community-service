-- Widen the role column to support comma-separated multi-role assignments.
-- The previous VARCHAR(20) silently truncated or rejected values like
-- "COMMUNITY_ADMIN, SPORTS_ADMIN" (29 chars), preventing multi-role saves.
ALTER TABLE app_user ALTER COLUMN role TYPE VARCHAR(255);
