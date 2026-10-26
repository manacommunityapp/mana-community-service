-- Phase 7: Settlement & Payouts — extended settlement tracking fields.

ALTER TABLE commerce_settlement
    ADD COLUMN IF NOT EXISTS processed_at         TIMESTAMP,
    ADD COLUMN IF NOT EXISTS admin_note           VARCHAR(300),
    ADD COLUMN IF NOT EXISTS bank_account_number  VARCHAR(40),
    ADD COLUMN IF NOT EXISTS bank_ifsc            VARCHAR(20),
    ADD COLUMN IF NOT EXISTS upi_id               VARCHAR(80),
    ADD COLUMN IF NOT EXISTS total_orders_count   INT NOT NULL DEFAULT 0;

-- Allow querying settlements by community + status for admin dashboard
CREATE INDEX IF NOT EXISTS idx_cs_community_status
    ON commerce_settlement(community_id, status);

CREATE INDEX IF NOT EXISTS idx_cs_seller_status
    ON commerce_settlement(seller_id, status)
    WHERE seller_id IS NOT NULL;
