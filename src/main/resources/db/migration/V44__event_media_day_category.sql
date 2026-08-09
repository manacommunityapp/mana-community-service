-- Day tags and categories for the event media gallery (community-scoped)
CREATE TABLE IF NOT EXISTS manacommunity.event_media_day (
  id          BIGSERIAL PRIMARY KEY,
  community_id BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
  label       VARCHAR(100) NOT NULL,
  sort_order  INT NOT NULL DEFAULT 0,
  created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_event_media_day_community
  ON manacommunity.event_media_day (community_id, sort_order);

CREATE TABLE IF NOT EXISTS manacommunity.event_media_category (
  id          BIGSERIAL PRIMARY KEY,
  community_id BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
  name        VARCHAR(100) NOT NULL,
  sort_order  INT NOT NULL DEFAULT 0,
  created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_event_media_category_community
  ON manacommunity.event_media_category (community_id, sort_order);

-- Add day_tag and category columns to the gallery item table
ALTER TABLE IF EXISTS manacommunity.event_gallery_item
  ADD COLUMN IF NOT EXISTS day_tag  VARCHAR(100),
  ADD COLUMN IF NOT EXISTS category VARCHAR(100);
