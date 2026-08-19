-- Link create-event image fields to centralized media_objects rows.
-- image_url/scanner_url remain as legacy URL fallbacks; media UUIDs are the durable S3-backed path.

ALTER TABLE IF EXISTS manacommunity.community_event
  ADD COLUMN IF NOT EXISTS image_media_external_id UUID,
  ADD COLUMN IF NOT EXISTS scanner_media_external_id UUID;

CREATE INDEX IF NOT EXISTS idx_community_event_image_media
  ON manacommunity.community_event (image_media_external_id);

CREATE INDEX IF NOT EXISTS idx_community_event_scanner_media
  ON manacommunity.community_event (scanner_media_external_id);
