-- V143: Personal Finance Phase 3.5 Enhancements
-- Adds tags and split_details to personal_finance_transactions.

ALTER TABLE personal_finance_transactions
ADD COLUMN IF NOT EXISTS tags VARCHAR(500),
ADD COLUMN IF NOT EXISTS split_details TEXT;

CREATE INDEX IF NOT EXISTS idx_pf_txn_tags ON personal_finance_transactions(tags);
