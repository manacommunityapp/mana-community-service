-- V194__fix_txn_intent_payer_index.sql
-- Fix index collision on transaction_intent(payer_id) with transaction_core_record(payer_id)

CREATE INDEX IF NOT EXISTS idx_txn_intent_payer ON transaction_intent(payer_id);

