-- Link legacy vendor/vendors table to mana_vendor master (best-effort, no mandatory FK to avoid breaking existing data)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'vendor') THEN
    EXECUTE 'ALTER TABLE vendor ADD COLUMN IF NOT EXISTS mana_vendor_id BIGINT';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_vendor_mana_vendor_id ON vendor(mana_vendor_id) WHERE mana_vendor_id IS NOT NULL';
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'vendors') THEN
    EXECUTE 'ALTER TABLE vendors ADD COLUMN IF NOT EXISTS mana_vendor_id BIGINT';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_vendors_mana_vendor_id ON vendors(mana_vendor_id) WHERE mana_vendor_id IS NOT NULL';
  END IF;
END $$;

-- Link finance_vendor / finance_vendors to mana_vendor
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'finance_vendor') THEN
    EXECUTE 'ALTER TABLE finance_vendor ADD COLUMN IF NOT EXISTS mana_vendor_id BIGINT';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_finance_vendor_mana ON finance_vendor(mana_vendor_id) WHERE mana_vendor_id IS NOT NULL';
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'finance_vendors') THEN
    EXECUTE 'ALTER TABLE finance_vendors ADD COLUMN IF NOT EXISTS mana_vendor_id BIGINT';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_finance_vendors_mana ON finance_vendors(mana_vendor_id) WHERE mana_vendor_id IS NOT NULL';
  END IF;
END $$;

-- Add PAN uniqueness constraint to mana_vendor (soft — unique index not unique constraint, to avoid breaking existing dupes)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'mana_vendor' AND column_name = 'pan') THEN
    CREATE UNIQUE INDEX IF NOT EXISTS idx_mana_vendor_pan_unique
        ON mana_vendor(pan)
        WHERE pan IS NOT NULL AND pan != '';
  ELSIF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'mana_vendor' AND column_name = 'pan_number') THEN
    CREATE UNIQUE INDEX IF NOT EXISTS idx_mana_vendor_pan_unique
        ON mana_vendor(pan_number)
        WHERE pan_number IS NOT NULL AND pan_number != '';
  END IF;
END $$;
