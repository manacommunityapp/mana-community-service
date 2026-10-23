-- Migration V166: Mana Marketplace Full Lifecycle — Seller KYC, Inventory Reservations, and Settlements

ALTER TABLE marketplace_listings ADD COLUMN IF NOT EXISTS available_quantity INT DEFAULT 1;

CREATE TABLE IF NOT EXISTS market_seller_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    business_name VARCHAR(150) NOT NULL,
    seller_type VARCHAR(50) NOT NULL DEFAULT 'HOMEPRENEUR',
    store_description TEXT,
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    flat_number VARCHAR(50),
    fssai_license_number VARCHAR(50),
    gstin VARCHAR(50),
    pan_number VARCHAR(50),
    bank_account_number VARCHAR(50),
    bank_ifsc_code VARCHAR(30),
    bank_account_holder_name VARCHAR(150),
    kyc_status VARCHAR(50) NOT NULL DEFAULT 'UNVERIFIED',
    kyc_rejection_reason TEXT,
    kyc_submitted_at TIMESTAMP,
    kyc_verified_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    is_open BOOLEAN DEFAULT TRUE,
    rating NUMERIC(3, 2) DEFAULT 5.00,
    total_reviews INT DEFAULT 0,
    total_orders_completed INT DEFAULT 0,
    community_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mkt_seller_comm ON market_seller_profiles(community_id);
CREATE INDEX IF NOT EXISTS idx_mkt_seller_kyc ON market_seller_profiles(kyc_status);
CREATE INDEX IF NOT EXISTS idx_mkt_seller_user ON market_seller_profiles(user_id);

CREATE TABLE IF NOT EXISTS market_inventory_reservations (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_number VARCHAR(100),
    quantity INT NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'RESERVED',
    reserved_until TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mkt_inv_res_listing ON market_inventory_reservations(listing_id);
CREATE INDEX IF NOT EXISTS idx_mkt_inv_res_status ON market_inventory_reservations(status);
CREATE INDEX IF NOT EXISTS idx_mkt_inv_res_expiry ON market_inventory_reservations(reserved_until);

CREATE TABLE IF NOT EXISTS market_settlement_records (
    id BIGSERIAL PRIMARY KEY,
    settlement_reference VARCHAR(100) UNIQUE NOT NULL,
    order_id BIGINT NOT NULL,
    order_number VARCHAR(100) NOT NULL,
    seller_id BIGINT NOT NULL,
    gross_amount NUMERIC(12, 2) NOT NULL,
    platform_fee NUMERIC(12, 2) DEFAULT 0.00,
    tax_deduction NUMERIC(12, 2) DEFAULT 0.00,
    net_payout_amount NUMERIC(12, 2) NOT NULL,
    payout_mode VARCHAR(50) NOT NULL DEFAULT 'WALLET',
    payout_reference VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_CLEARANCE',
    settled_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mkt_settle_seller ON market_settlement_records(seller_id);
CREATE INDEX IF NOT EXISTS idx_mkt_settle_status ON market_settlement_records(status);
CREATE INDEX IF NOT EXISTS idx_mkt_settle_order ON market_settlement_records(order_id);
