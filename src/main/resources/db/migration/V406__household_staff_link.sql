-- V406: Home help staff linked to residential flats
-- Full staff registry (attendance, tasks) will be added by ops agent separately

CREATE TABLE IF NOT EXISTS household_staff_link (
  id              BIGSERIAL PRIMARY KEY,
  flat_id         BIGINT NOT NULL REFERENCES community_flat(id),
  staff_name      VARCHAR(200) NOT NULL,
  staff_type      VARCHAR(30) NOT NULL, -- MAID, COOK, DRIVER, NANNY, GARDENER, OTHER
  phone           VARCHAR(20),
  external_ref_id BIGINT,              -- FK to full staff registry when added
  active          BOOLEAN NOT NULL DEFAULT TRUE,
  since_date      DATE,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_staff_link_flat   ON household_staff_link(flat_id);
CREATE INDEX IF NOT EXISTS idx_staff_link_active ON household_staff_link(flat_id, active);
