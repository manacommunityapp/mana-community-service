-- Link legacy vendor table to mana_vendor master (best-effort, no mandatory FK to avoid breaking existing data)
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS mana_vendor_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_vendor_mana_vendor_id ON vendor(mana_vendor_id) WHERE mana_vendor_id IS NOT NULL;
COMMENT ON COLUMN vendor.mana_vendor_id IS 'FK to mana_vendor.id — links legacy vendor record to unified vendor master';

-- Link finance_vendor to mana_vendor
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'finance_vendor') THEN
    EXECUTE 'ALTER TABLE finance_vendor ADD COLUMN IF NOT EXISTS mana_vendor_id BIGINT';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_finance_vendor_mana ON finance_vendor(mana_vendor_id) WHERE mana_vendor_id IS NOT NULL';
  END IF;
END $$;

-- Add PAN uniqueness constraint to mana_vendor (soft — unique index not unique constraint, to avoid breaking existing dupes)
CREATE UNIQUE INDEX IF NOT EXISTS idx_mana_vendor_pan_unique
    ON mana_vendor(pan_number)
    WHERE pan_number IS NOT NULL AND pan_number != '';
