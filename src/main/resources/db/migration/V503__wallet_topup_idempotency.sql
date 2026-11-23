-- V503: make wallet top-ups idempotent at the database level.
--
-- Before this, POST /api/wallet/topup/confirm credited the wallet on every call for
-- the same gateway reference, so a resident could replay one callback into unlimited
-- spendable balance. The service now consumes the PENDING top-up row created at
-- initiation exactly once; this index makes that invariant the database's job, for
-- every present and future writer of community_wallet_transactions.
--
-- Shape: a top-up reference may appear on at most one COMPLETED TOPUP row. PENDING
-- (not yet confirmed), FAILED and non-top-up rows are deliberately outside the index.

-- Refuse to install a half-enforced constraint: if historical data already contains
-- a reference credited more than once, stop the deploy with an actionable message
-- rather than silently leaving the hole open.
DO $$
DECLARE
    dup_count integer;
BEGIN
    SELECT count(*) INTO dup_count
    FROM (
        SELECT external_txn_ref
        FROM community_wallet_transactions
        WHERE txn_type = 'TOPUP'
          AND status = 'COMPLETED'
          AND external_txn_ref IS NOT NULL
        GROUP BY external_txn_ref
        HAVING count(*) > 1
    ) duplicates;

    IF dup_count > 0 THEN
        RAISE EXCEPTION
            'Cannot enforce wallet top-up idempotency: % external_txn_ref value(s) already carry more than one COMPLETED top-up credit. Reconcile/reverse the duplicate credits in community_wallet_transactions, then re-run this migration.',
            dup_count;
    END IF;
END $$;

-- One confirmed credit per gateway reference.
CREATE UNIQUE INDEX IF NOT EXISTS ux_wallet_txn_topup_external_ref
    ON community_wallet_transactions (external_txn_ref)
    WHERE txn_type = 'TOPUP'
      AND status = 'COMPLETED'
      AND external_txn_ref IS NOT NULL;

-- Supports the confirmation lookup by reference (findByExternalTxnRef).
CREATE INDEX IF NOT EXISTS idx_wallet_txn_external_ref
    ON community_wallet_transactions (external_txn_ref)
    WHERE external_txn_ref IS NOT NULL;
