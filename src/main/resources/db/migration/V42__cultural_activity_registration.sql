ALTER TABLE IF EXISTS manacommunity.event_program
  ADD COLUMN IF NOT EXISTS capacity INT,
  ADD COLUMN IF NOT EXISTS requires_registration BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS allow_waitlist BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN IF NOT EXISTS max_per_registration INT NOT NULL DEFAULT 10;

ALTER TABLE IF EXISTS manacommunity.community_event_activity_registration
  ADD COLUMN IF NOT EXISTS registration_type VARCHAR(30) DEFAULT 'individual',
  ADD COLUMN IF NOT EXISTS primary_name VARCHAR(200),
  ADD COLUMN IF NOT EXISTS primary_email VARCHAR(200),
  ADD COLUMN IF NOT EXISTS primary_phone VARCHAR(50),
  ADD COLUMN IF NOT EXISTS participant_data TEXT,
  ADD COLUMN IF NOT EXISTS custom_data TEXT,
  ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100),
  ADD COLUMN IF NOT EXISTS waitlist_position INT,
  ADD COLUMN IF NOT EXISTS decision_reason VARCHAR(500),
  ADD COLUMN IF NOT EXISTS approved_by BIGINT REFERENCES manacommunity.app_user(id),
  ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_activity_reg_status
  ON manacommunity.community_event_activity_registration (program_id, status);

CREATE INDEX IF NOT EXISTS idx_activity_reg_idempotency
  ON manacommunity.community_event_activity_registration (program_id, user_id, idempotency_key);
