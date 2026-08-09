-- Add optional metadata fields to community_event and introduce draft/publish status
ALTER TABLE IF EXISTS manacommunity.community_event
  ADD COLUMN IF NOT EXISTS venue        VARCHAR(200),
  ADD COLUMN IF NOT EXISTS city         VARCHAR(100),
  ADD COLUMN IF NOT EXISTS category     VARCHAR(100),
  ADD COLUMN IF NOT EXISTS status       VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  ADD COLUMN IF NOT EXISTS max_attendees INT;

CREATE INDEX IF NOT EXISTS idx_community_event_status
  ON manacommunity.community_event (community_id, status);
