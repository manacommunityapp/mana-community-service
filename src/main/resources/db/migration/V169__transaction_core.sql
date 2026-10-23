-- V160__transaction_core.sql
-- MANA Transaction Core tables

CREATE TABLE IF NOT EXISTS transaction_core_record (
    id BIGSERIAL PRIMARY KEY,
    transaction_number VARCHAR(64) NOT NULL UNIQUE,
    domain VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    amount NUMERIC(18, 2) NOT NULL,
    net_amount NUMERIC(18, 2) NOT NULL,
    tax_amount NUMERIC(18, 2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(18, 2) NOT NULL DEFAULT 0.00,
    platform_fee NUMERIC(18, 2) NOT NULL DEFAULT 0.00,
    tds_amount NUMERIC(18, 2) NOT NULL DEFAULT 0.00,
    vendor_payout_amount NUMERIC(18, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    
    payer_id BIGINT NOT NULL,
    payee_id BIGINT,
    community_id BIGINT,
    property_id BIGINT,
    
    reference_type VARCHAR(64),
    reference_id VARCHAR(64),
    idempotency_key VARCHAR(128),
    
    is_escrow BOOLEAN NOT NULL DEFAULT FALSE,
    escrow_released BOOLEAN NOT NULL DEFAULT FALSE,
    escrow_released_at TIMESTAMP,
    
    payment_id BIGINT,
    invoice_id BIGINT,
    receipt_id BIGINT,
    journal_entry_id BIGINT,
    settlement_id BIGINT,
    refund_id BIGINT,
    
    gateway_reference VARCHAR(128),
    narration TEXT,
    reconciliation_status VARCHAR(30) DEFAULT 'PENDING',
    reconciliation_utr VARCHAR(128),
    reconciled_at TIMESTAMP,
    
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_txn_domain ON transaction_core_record(domain);
CREATE INDEX IF NOT EXISTS idx_txn_status ON transaction_core_record(status);
CREATE INDEX IF NOT EXISTS idx_txn_payer ON transaction_core_record(payer_id);
CREATE INDEX IF NOT EXISTS idx_txn_payee ON transaction_core_record(payee_id);
CREATE INDEX IF NOT EXISTS idx_txn_ref ON transaction_core_record(reference_type, reference_id);
