CREATE TABLE IF NOT EXISTS manacommunity.commerce_order (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(40) UNIQUE NOT NULL,
    channel VARCHAR(32) NOT NULL,
    buyer_id BIGINT NOT NULL REFERENCES manacommunity.app_user(id) ON DELETE CASCADE,
    seller_id BIGINT REFERENCES manacommunity.app_user(id) ON DELETE SET NULL,
    vendor_id VARCHAR(64),
    vendor_name VARCHAR(150),
    community_id BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    channel_reference_id VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED',
    subtotal_amount NUMERIC(12, 2) NOT NULL,
    discount_amount NUMERIC(12, 2) DEFAULT 0,
    delivery_fee NUMERIC(12, 2) DEFAULT 0,
    tax_amount NUMERIC(12, 2) DEFAULT 0,
    total_amount NUMERIC(12, 2) NOT NULL,
    savings_amount NUMERIC(12, 2) DEFAULT 0,
    payment_method VARCHAR(32) NOT NULL DEFAULT 'UPI',
    payment_status VARCHAR(32) NOT NULL DEFAULT 'INITIATED',
    fulfillment_type VARCHAR(32) NOT NULL DEFAULT 'CLUBHOUSE_PICKUP',
    delivery_address TEXT,
    pickup_point VARCHAR(150),
    pickup_slot VARCHAR(80),
    handover_otp VARCHAR(8),
    qr_token VARCHAR(120) UNIQUE,
    is_escrow_locked BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_comm_order_buyer ON manacommunity.commerce_order(buyer_id);
CREATE INDEX IF NOT EXISTS idx_comm_order_comm ON manacommunity.commerce_order(community_id);
CREATE INDEX IF NOT EXISTS idx_comm_order_status ON manacommunity.commerce_order(status);
CREATE INDEX IF NOT EXISTS idx_comm_order_chan ON manacommunity.commerce_order(channel);

CREATE TABLE IF NOT EXISTS manacommunity.commerce_order_item (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES manacommunity.commerce_order(id) ON DELETE CASCADE,
    product_id VARCHAR(64),
    variant_id VARCHAR(64),
    sku VARCHAR(64),
    title VARCHAR(200) NOT NULL,
    pack_size VARCHAR(50),
    unit_price NUMERIC(12, 2) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    tax_rate NUMERIC(5, 2) DEFAULT 0,
    tax_amount NUMERIC(12, 2) DEFAULT 0,
    total_price NUMERIC(12, 2) NOT NULL,
    image_url TEXT
);

CREATE INDEX IF NOT EXISTS idx_comm_item_order ON manacommunity.commerce_order_item(order_id);

CREATE TABLE IF NOT EXISTS manacommunity.commerce_payment (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES manacommunity.commerce_order(id) ON DELETE CASCADE,
    payment_ref VARCHAR(80) UNIQUE NOT NULL,
    gateway VARCHAR(32) NOT NULL DEFAULT 'RAZORPAY',
    payment_method VARCHAR(32) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(8) NOT NULL DEFAULT 'INR',
    status VARCHAR(32) NOT NULL DEFAULT 'CAPTURED',
    gateway_signature TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS manacommunity.commerce_handover_pass (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES manacommunity.commerce_order(id) ON DELETE CASCADE,
    pass_code VARCHAR(64) UNIQUE NOT NULL,
    qr_payload TEXT NOT NULL,
    otp VARCHAR(8) NOT NULL,
    fulfillment_type VARCHAR(32) NOT NULL,
    pickup_point VARCHAR(150),
    verified_by_user_id BIGINT REFERENCES manacommunity.app_user(id),
    verified_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS manacommunity.commerce_review (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES manacommunity.commerce_order(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES manacommunity.app_user(id) ON DELETE CASCADE,
    channel VARCHAR(32) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    rating INT NOT NULL,
    quality_score INT,
    on_time_score INT,
    comment TEXT,
    photos_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS manacommunity.commerce_dispute (
    id BIGSERIAL PRIMARY KEY,
    dispute_code VARCHAR(64) UNIQUE NOT NULL,
    order_id BIGINT NOT NULL REFERENCES manacommunity.commerce_order(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES manacommunity.app_user(id) ON DELETE CASCADE,
    channel VARCHAR(32) NOT NULL,
    reason VARCHAR(64) NOT NULL,
    description TEXT NOT NULL,
    requested_resolution VARCHAR(64) NOT NULL,
    claim_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
    vendor_response TEXT,
    evidence_urls_json TEXT,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS manacommunity.commerce_settlement (
    id BIGSERIAL PRIMARY KEY,
    settlement_number VARCHAR(64) UNIQUE NOT NULL,
    vendor_id VARCHAR(64),
    seller_id BIGINT REFERENCES manacommunity.app_user(id),
    community_id BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    cycle_start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    cycle_end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    total_orders_count INT NOT NULL DEFAULT 0,
    gross_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    platform_fee NUMERIC(12, 2) NOT NULL DEFAULT 0,
    deductions NUMERIC(12, 2) NOT NULL DEFAULT 0,
    net_payout NUMERIC(12, 2) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    payout_utr VARCHAR(64),
    payout_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);