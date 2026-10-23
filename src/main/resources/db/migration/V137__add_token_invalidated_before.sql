-- V137: Add token_invalidated_before column to app_user.
-- When set, any JWT issued before this timestamp is rejected on the server side.
-- Used to invalidate all sessions on password change/reset (OWASP ASVS 3.3.1).
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS token_invalidated_before TIMESTAMP;
