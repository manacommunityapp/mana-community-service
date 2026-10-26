-- Phase 4: Order lifecycle extensions

ALTER TABLE commerce_order
    ADD COLUMN IF NOT EXISTS customer_profile_id     BIGINT REFERENCES commerce_customer_profile(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS notes                   TEXT,
    ADD COLUMN IF NOT EXISTS vendor_notes            TEXT,
    ADD COLUMN IF NOT EXISTS estimated_delivery_date DATE,
    ADD COLUMN IF NOT EXISTS actual_delivery_at      TIMESTAMP,
    ADD COLUMN IF NOT EXISTS cancellation_reason     VARCHAR(300);

CREATE INDEX IF NOT EXISTS idx_co_customer_profile ON commerce_order(customer_profile_id)
    WHERE customer_profile_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_co_vendor_id_status  ON commerce_order(vendor_id, status);
CREATE INDEX IF NOT EXISTS idx_co_community_status  ON commerce_order(community_id, status);
CREATE INDEX IF NOT EXISTS idx_co_buyer_status       ON commerce_order(buyer_id, status);
