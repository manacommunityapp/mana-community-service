-- =============================================================================
-- Migration: V271__fresh_precautionary_safeguards.sql
-- Description: Precautionary safeguards for dietary segregation (FSSAI Schedule 4),
--              and catch-weight overweight pending surcharge recovery.
-- =============================================================================

-- 1. Dietary category on fulfilment groups to enforce physical bag segregation
ALTER TABLE manacommunity.fresh_fulfilment_group
    ADD COLUMN IF NOT EXISTS dietary_category VARCHAR(30) NOT NULL DEFAULT 'VEGETARIAN';

CREATE INDEX IF NOT EXISTS idx_fresh_fulfilment_group_dietary
    ON manacommunity.fresh_fulfilment_group (dietary_category);

-- 2. Pending surcharge recovery columns on weight adjustment log
ALTER TABLE manacommunity.fresh_weight_adjustment_log
    ADD COLUMN IF NOT EXISTS is_surcharge_pending BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS pending_surcharge_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00;

CREATE INDEX IF NOT EXISTS idx_fresh_weight_adj_pending
    ON manacommunity.fresh_weight_adjustment_log (customer_id, is_surcharge_pending);
