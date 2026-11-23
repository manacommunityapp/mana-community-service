-- Make property_id and resident_id nullable so cfbos_invoice can represent
-- non-community invoices (events, sports, marketplace, etc.).
-- Add community_id and source-module columns for multi-module support.

ALTER TABLE cfbos_invoice ALTER COLUMN property_id DROP NOT NULL;
ALTER TABLE cfbos_invoice ALTER COLUMN resident_id DROP NOT NULL;

ALTER TABLE cfbos_invoice ADD COLUMN IF NOT EXISTS community_id BIGINT;
ALTER TABLE cfbos_invoice ADD COLUMN IF NOT EXISTS source_module VARCHAR(30);
ALTER TABLE cfbos_invoice ADD COLUMN IF NOT EXISTS source_reference_type VARCHAR(50);
ALTER TABLE cfbos_invoice ADD COLUMN IF NOT EXISTS source_reference_id BIGINT;

-- Backfill community_id from existing resident/property relationship where possible.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'property') THEN
        UPDATE cfbos_invoice i
        SET community_id = (
            SELECT p.community_id FROM property p WHERE p.id = i.property_id LIMIT 1
        )
        WHERE i.community_id IS NULL AND i.property_id IS NOT NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_cfbos_invoice_community_id ON cfbos_invoice (community_id);
CREATE INDEX IF NOT EXISTS idx_cfbos_invoice_source ON cfbos_invoice (source_module, source_reference_type, source_reference_id);
