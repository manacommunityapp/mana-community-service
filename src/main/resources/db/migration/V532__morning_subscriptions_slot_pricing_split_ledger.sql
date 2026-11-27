-- Early-Morning Subscriptions (Cutoff + Manifest + Auto-Refund), Slot Contention & Multi-Tender Split Ledger

ALTER TABLE hs_recurring_plan
    ADD COLUMN IF NOT EXISTS sku_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS vendor_id BIGINT,
    ADD COLUMN IF NOT EXISTS tower_block VARCHAR(32),
    ADD COLUMN IF NOT EXISTS floor_number INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS flat_number VARCHAR(32),
    ADD COLUMN IF NOT EXISTS default_qty INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS unit_price NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS vacation_from DATE,
    ADD COLUMN IF NOT EXISTS vacation_until DATE;

CREATE TABLE IF NOT EXISTS morning_sub_overrides (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    delivery_date DATE NOT NULL,
    override_qty INT NOT NULL DEFAULT 1,
    paused BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_morning_override_plan_date UNIQUE (plan_id, delivery_date)
);

CREATE TABLE IF NOT EXISTS morning_delivery_instances (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    community_id BIGINT NOT NULL,
    vendor_id BIGINT,
    user_id BIGINT NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    tower_block VARCHAR(32),
    floor_number INT NOT NULL DEFAULT 0,
    flat_number VARCHAR(32) NOT NULL,
    delivery_date DATE NOT NULL,
    qty INT NOT NULL DEFAULT 1,
    amount NUMERIC(10, 2) NOT NULL,
    wallet_hold_ref VARCHAR(128),
    wallet_status VARCHAR(32) NOT NULL DEFAULT 'RESERVED',
    delivery_status VARCHAR(32) NOT NULL DEFAULT 'FROZEN_FOR_DISPATCH',
    failure_reason VARCHAR(255),
    frozen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    settled_at TIMESTAMP,
    CONSTRAINT uq_morning_instance_plan_date UNIQUE (plan_id, delivery_date)
);

CREATE INDEX IF NOT EXISTS idx_morning_instance_vendor_date
    ON morning_delivery_instances (vendor_id, delivery_date, delivery_status);

ALTER TABLE hs_worker_availability
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS hold_token VARCHAR(64),
    ADD COLUMN IF NOT EXISTS hold_expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS travel_buffer_minutes INT NOT NULL DEFAULT 15;

CREATE TABLE IF NOT EXISTS multi_tender_settlements (
    id BIGSERIAL PRIMARY KEY,
    order_ref VARCHAR(64) NOT NULL UNIQUE,
    booking_id BIGINT,
    community_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    gross_amount NUMERIC(12, 2) NOT NULL,
    points_used NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    coupon_code VARCHAR(64),
    coupon_discount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    upi_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    platform_commission NUMERIC(12, 2) NOT NULL,
    commission_gst NUMERIC(12, 2) NOT NULL,
    vendor_net_payout NUMERIC(12, 2) NOT NULL,
    state VARCHAR(32) NOT NULL DEFAULT 'INITIATED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS settlement_ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    settlement_id BIGINT NOT NULL,
    account_code VARCHAR(64) NOT NULL,
    entry_type VARCHAR(16) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_settlement_ledger_sid
    ON settlement_ledger_entries (settlement_id);
