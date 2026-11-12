-- Add idempotency key column to kitty financial audit for duplicate request protection
ALTER TABLE manacommunity.kitty_financial_audit
    ADD COLUMN idempotency_key VARCHAR(80);

CREATE UNIQUE INDEX uq_kitty_audit_idempotency
    ON manacommunity.kitty_financial_audit (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
