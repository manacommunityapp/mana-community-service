-- V234: Add icon_url to sports_meta and use partial unique index for active sports
-- Prevents 409 DATA_CONFLICT when creating or re-activating sports.

-- 1. Ensure icon_url and icon are TEXT to support base64 image data URLs
ALTER TABLE manacommunity.sports_meta ADD COLUMN IF NOT EXISTS icon_url TEXT;
ALTER TABLE manacommunity.sports_meta ALTER COLUMN icon_url TYPE TEXT;
ALTER TABLE manacommunity.sports_meta ALTER COLUMN icon TYPE TEXT;

-- 2. Drop the old full unique index that blocked deactivated/soft-deleted sports
DROP INDEX IF EXISTS manacommunity.uq_sports_meta_name_community;

-- 3. Create partial unique index on active sports only
CREATE UNIQUE INDEX IF NOT EXISTS uq_sports_meta_name_community_active
    ON manacommunity.sports_meta (LOWER(name), COALESCE(community_id, 0))
    WHERE active = true;
