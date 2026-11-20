-- V405: Link vehicle_profile to community_flat for household graph
-- vehicle_profile already exists (V225); this adds the flat dimension

ALTER TABLE vehicle_profile
  ADD COLUMN IF NOT EXISTS flat_id       BIGINT REFERENCES community_flat(id),
  ADD COLUMN IF NOT EXISTS rfid_tag      VARCHAR(100),
  ADD COLUMN IF NOT EXISTS parking_slot  VARCHAR(50),
  ADD COLUMN IF NOT EXISTS is_active     BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN IF NOT EXISTS registered_at TIMESTAMPTZ DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_vehicle_profile_flat ON vehicle_profile(flat_id) WHERE flat_id IS NOT NULL;
