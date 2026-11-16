-- =============================================================================
-- Migration: V270__fresh_financial_integrity_and_audit.sql
-- Description: Financial integrity, wallet transaction linkage, and statutory
--              deduction columns for vendor settlements.
-- =============================================================================

-- 1. Wallet transaction reference on weight adjustments
ALTER TABLE manacommunity.fresh_weight_adjustment_log
    ADD COLUMN IF NOT EXISTS wallet_transaction_ref VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_fwadj_wallet_ref
    ON manacommunity.fresh_weight_adjustment_log (wallet_transaction_ref);

-- 2. Wallet transaction reference on recall orders
ALTER TABLE manacommunity.fresh_product_recall_order
    ADD COLUMN IF NOT EXISTS wallet_transaction_ref VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_fprecall_wallet_ref
    ON manacommunity.fresh_product_recall_order (wallet_transaction_ref);

-- 3. Statutory deduction columns on vendor settlements (TDS 194-O, GST, clawbacks)
ALTER TABLE manacommunity.fresh_vendor_settlement
    ADD COLUMN IF NOT EXISTS tds_deducted_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS gst_on_commission_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS refund_clawbacks_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00;
