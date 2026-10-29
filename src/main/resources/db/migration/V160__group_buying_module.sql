-- V159: Group Buying & Community Commerce Network Tables
-- Creates tables for Group Deals, Tiered Pricing, Orders, Community Demands, and Vendor Offers.

CREATE TABLE IF NOT EXISTS manacommunity.group_buy_deal (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(80) NOT NULL,
    sub_category VARCHAR(80),
    description TEXT,
    image_url TEXT,
    vendor_name VARCHAR(120) NOT NULL,
    vendor_id VARCHAR(60),
    vendor_rating NUMERIC(3, 2) DEFAULT 4.8,
    vendor_verified BOOLEAN DEFAULT TRUE,
    pricing_model VARCHAR(30) NOT NULL DEFAULT 'THRESHOLD',
    pricing_type VARCHAR(30) NOT NULL DEFAULT 'QUANTITY',
    mrp NUMERIC(12, 2) NOT NULL,
    standard_price NUMERIC(12, 2) NOT NULL,
    current_price NUMERIC(12, 2) NOT NULL,
    current_tier_price NUMERIC(12, 2),
    committed_qty INTEGER NOT NULL DEFAULT 0,
    target_qty INTEGER NOT NULL,
    current_participants INTEGER NOT NULL DEFAULT 0,
    target_participants INTEGER,
    inventory_remaining INTEGER,
    moq_label VARCHAR(100),
    deal_status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    deal_ends_at TIMESTAMP NOT NULL,
    price_locked_at TIMESTAMP,
    pickup_point VARCHAR(150),
    pickup_date TIMESTAMP,
    fulfillment_type VARCHAR(30) DEFAULT 'BOTH',
    payment_type VARCHAR(30) DEFAULT 'FULL',
    is_trending BOOLEAN DEFAULT FALSE,
    is_almost_unlocked BOOLEAN DEFAULT FALSE,
    is_festival_deal BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_group_buy_deal_community ON manacommunity.group_buy_deal(community_id);
CREATE INDEX IF NOT EXISTS idx_group_buy_deal_status ON manacommunity.group_buy_deal(deal_status);
CREATE INDEX IF NOT EXISTS idx_group_buy_deal_trending ON manacommunity.group_buy_deal(is_trending);
CREATE INDEX IF NOT EXISTS idx_group_buy_deal_almost ON manacommunity.group_buy_deal(is_almost_unlocked);

CREATE TABLE IF NOT EXISTS manacommunity.group_buy_deal_tier (
    id BIGSERIAL PRIMARY KEY,
    deal_id BIGINT NOT NULL REFERENCES manacommunity.group_buy_deal(id) ON DELETE CASCADE,
    min_qty INTEGER NOT NULL,
    max_qty INTEGER,
    price NUMERIC(12, 2) NOT NULL,
    label VARCHAR(60)
);

CREATE INDEX IF NOT EXISTS idx_group_buy_deal_tier_deal ON manacommunity.group_buy_deal_tier(deal_id);

CREATE TABLE IF NOT EXISTS manacommunity.group_buy_order (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(40) UNIQUE NOT NULL,
    community_id BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES manacommunity.app_user(id) ON DELETE CASCADE,
    deal_id BIGINT NOT NULL REFERENCES manacommunity.group_buy_deal(id) ON DELETE CASCADE,
    deal_title VARCHAR(200) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price NUMERIC(12, 2) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    savings_amount NUMERIC(12, 2) DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
    qr_token VARCHAR(100) UNIQUE,
    pickup_point VARCHAR(150),
    pickup_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_group_buy_order_user ON manacommunity.group_buy_order(user_id);
CREATE INDEX IF NOT EXISTS idx_group_buy_order_deal ON manacommunity.group_buy_order(deal_id);
CREATE INDEX IF NOT EXISTS idx_group_buy_order_community ON manacommunity.group_buy_order(community_id);
CREATE INDEX IF NOT EXISTS idx_group_buy_order_qr ON manacommunity.group_buy_order(qr_token);

CREATE TABLE IF NOT EXISTS manacommunity.group_buy_demand (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    created_by_user_id BIGINT NOT NULL REFERENCES manacommunity.app_user(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    category VARCHAR(80) NOT NULL,
    description TEXT,
    interested_residents INTEGER NOT NULL DEFAULT 1,
    expected_qty INTEGER DEFAULT 1,
    upvotes_count INTEGER NOT NULL DEFAULT 1,
    target_upvotes INTEGER DEFAULT 25,
    preferred_price_min NUMERIC(12, 2),
    preferred_price_max NUMERIC(12, 2),
    preferred_brand VARCHAR(100),
    preferred_pack_size VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_group_buy_demand_community ON manacommunity.group_buy_demand(community_id);
CREATE INDEX IF NOT EXISTS idx_group_buy_demand_status ON manacommunity.group_buy_demand(status);

CREATE TABLE IF NOT EXISTS manacommunity.group_buy_demand_offer (
    id BIGSERIAL PRIMARY KEY,
    demand_id BIGINT NOT NULL REFERENCES manacommunity.group_buy_demand(id) ON DELETE CASCADE,
    vendor_id VARCHAR(60),
    vendor_name VARCHAR(120) NOT NULL,
    vendor_rating NUMERIC(3, 2) DEFAULT 4.9,
    vendor_verified BOOLEAN DEFAULT TRUE,
    offered_price NUMERIC(12, 2) NOT NULL,
    minimum_qty INTEGER NOT NULL,
    maximum_qty INTEGER,
    delivery_date DATE,
    is_best_value BOOLEAN DEFAULT FALSE,
    terms TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_group_buy_demand_offer_demand ON manacommunity.group_buy_demand_offer(demand_id);
