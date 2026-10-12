-- V186__mana_pay_intent_fields.sql
-- Add unified payment capability fields to transaction_intent

CREATE TABLE IF NOT EXISTS transaction_intent (
    id BIGSERIAL PRIMARY KEY,
    intent_id VARCHAR(64) NOT NULL UNIQUE,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    domain VARCHAR(32) NOT NULL,
    order_reference_id VARCHAR(64) NOT NULL,
    community_id BIGINT,
    payer_id BIGINT NOT NULL DEFAULT 0,
    payer_name VARCHAR(150),
    payer_email VARCHAR(150),
    payer_phone VARCHAR(30),
    payer_unit VARCHAR(100),
    title VARCHAR(255),
    amount NUMERIC(14, 2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'INR',
    status VARCHAR(32) NOT NULL DEFAULT 'INITIATED',
    payment_rail VARCHAR(32) DEFAULT 'UPI',
    wallet_deduction_amount NUMERIC(14, 2) DEFAULT 0.00,
    gateway_amount NUMERIC(14, 2) DEFAULT 0.00,
    gateway_order_id VARCHAR(100),
    gateway_payment_id VARCHAR(100),
    utr VARCHAR(64),
    upi_vpa VARCHAR(100),
    upi_intent_uri VARCHAR(500),
    paid_at TIMESTAMP,
    escrow_held BOOLEAN DEFAULT FALSE,
    escrow_condition VARCHAR(32),
    journal_entry_number VARCHAR(64),
    receipt_number VARCHAR(64),
    invoice_number VARCHAR(64),
    splits_json JSONB,
    metadata_json JSONB,
    remarks VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    expires_at TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='transaction_intent' AND column_name='title') THEN
        ALTER TABLE transaction_intent ADD COLUMN title VARCHAR(255);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='transaction_intent' AND column_name='payer_unit') THEN
        ALTER TABLE transaction_intent ADD COLUMN payer_unit VARCHAR(100);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='transaction_intent' AND column_name='utr') THEN
        ALTER TABLE transaction_intent ADD COLUMN utr VARCHAR(64);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='transaction_intent' AND column_name='upi_vpa') THEN
        ALTER TABLE transaction_intent ADD COLUMN upi_vpa VARCHAR(100);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='transaction_intent' AND column_name='upi_intent_uri') THEN
        ALTER TABLE transaction_intent ADD COLUMN upi_intent_uri VARCHAR(500);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='transaction_intent' AND column_name='paid_at') THEN
        ALTER TABLE transaction_intent ADD COLUMN paid_at TIMESTAMP;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_txn_intent_id ON transaction_intent(intent_id);
CREATE INDEX IF NOT EXISTS idx_txn_idempotency_key ON transaction_intent(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_txn_order_ref ON transaction_intent(order_reference_id);
CREATE INDEX IF NOT EXISTS idx_txn_payer ON transaction_intent(payer_id);
CREATE INDEX IF NOT EXISTS idx_txn_domain_status ON transaction_intent(domain, status);
