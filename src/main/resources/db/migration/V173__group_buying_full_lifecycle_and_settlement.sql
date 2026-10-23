-- V173: Group Buying Full Lifecycle, Fulfillment, Doorstep Delivery, Refunds, and Vendor Settlement
ALTER TABLE manacommunity.group_buy_order
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(30) DEFAULT 'PAID',
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50) DEFAULT 'UPI',
    ADD COLUMN IF NOT EXISTS transaction_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS payment_timestamp TIMESTAMP,
    ADD COLUMN IF NOT EXISTS delivery_otp VARCHAR(10),
    ADD COLUMN IF NOT EXISTS delivery_partner_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS delivery_partner_phone VARCHAR(30),
    ADD COLUMN IF NOT EXISTS tracking_number VARCHAR(80),
    ADD COLUMN IF NOT EXISTS delivery_timestamp TIMESTAMP,
    ADD COLUMN IF NOT EXISTS refund_amount NUMERIC(12, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS refund_reason VARCHAR(255),
    ADD COLUMN IF NOT EXISTS refund_timestamp TIMESTAMP,
    ADD COLUMN IF NOT EXISTS tier_price_refund_amount NUMERIC(12, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS settlement_id BIGINT,
    ADD COLUMN IF NOT EXISTS is_settled BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_group_buy_order_settlement ON manacommunity.group_buy_order(settlement_id);
CREATE INDEX IF NOT EXISTS idx_group_buy_order_status ON manacommunity.group_buy_order(status);

CREATE TABLE IF NOT EXISTS manacommunity.group_buy_settlement (
    id BIGSERIAL PRIMARY KEY,
    deal_id BIGINT NOT NULL REFERENCES manacommunity.group_buy_deal(id) ON DELETE CASCADE,
    deal_title VARCHAR(200) NOT NULL,
    community_id BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    vendor_id VARCHAR(80) NOT NULL,
    vendor_name VARCHAR(120) NOT NULL,
    total_orders INTEGER NOT NULL DEFAULT 0,
    total_quantity INTEGER NOT NULL DEFAULT 0,
    gross_sales_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    platform_commission_rate NUMERIC(5, 2) NOT NULL DEFAULT 3.00,
    platform_commission_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    community_reserve_rate NUMERIC(5, 2) NOT NULL DEFAULT 1.00,
    community_reserve_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    tier_refunds_total NUMERIC(12, 2) NOT NULL DEFAULT 0,
    net_vendor_payout NUMERIC(12, 2) NOT NULL DEFAULT 0,
    settlement_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payout_reference VARCHAR(100),
    settled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_group_buy_settlement_deal ON manacommunity.group_buy_settlement(deal_id);
CREATE INDEX IF NOT EXISTS idx_group_buy_settlement_vendor ON manacommunity.group_buy_settlement(vendor_id);
CREATE INDEX IF NOT EXISTS idx_group_buy_settlement_community ON manacommunity.group_buy_settlement(community_id);
