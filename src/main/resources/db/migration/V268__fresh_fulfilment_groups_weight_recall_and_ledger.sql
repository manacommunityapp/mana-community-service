-- =============================================================================
-- Migration: V268__fresh_fulfilment_groups_weight_recall_and_ledger.sql
-- Description: Mana Fresh Phase B - Order Splitting into Fulfilment Groups,
--              Weight Variation Ledger, Product Recall Engine, Wastage Tracking,
--              and Immutable Stock Movement Ledger.
-- Schema: manacommunity
-- =============================================================================

-- 1. Fresh Fulfilment Groups
CREATE TABLE IF NOT EXISTS manacommunity.fresh_fulfilment_group (
    id BIGSERIAL PRIMARY KEY,
    group_reference VARCHAR(50) UNIQUE NOT NULL,
    order_id BIGINT NOT NULL REFERENCES manacommunity.commerce_order(id) ON DELETE CASCADE,
    group_number INT NOT NULL DEFAULT 1,
    vendor_id BIGINT REFERENCES manacommunity.vms_vendors(id),
    fulfilment_owner_type VARCHAR(50) NOT NULL,
    delivery_mode VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    fulfilment_location_id BIGINT REFERENCES manacommunity.fresh_fulfilment_location(id),
    target_delivery_time TIMESTAMP WITH TIME ZONE,
    dispatched_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    tracking_reference VARCHAR(100),
    handover_otp VARCHAR(10),
    qr_code_token VARCHAR(255),
    subtotal_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    total_weight_g NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    requires_cold_chain BOOLEAN NOT NULL DEFAULT FALSE,
    metadata_json JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fresh_fulfilment_group_order_id ON manacommunity.fresh_fulfilment_group(order_id);
CREATE INDEX IF NOT EXISTS idx_fresh_fulfilment_group_vendor_id ON manacommunity.fresh_fulfilment_group(vendor_id);
CREATE INDEX IF NOT EXISTS idx_fresh_fulfilment_group_status ON manacommunity.fresh_fulfilment_group(status);
CREATE INDEX IF NOT EXISTS idx_fresh_fulfilment_group_location ON manacommunity.fresh_fulfilment_group(fulfilment_location_id);

-- 2. Fresh Fulfilment Items
CREATE TABLE IF NOT EXISTS manacommunity.fresh_fulfilment_item (
    id BIGSERIAL PRIMARY KEY,
    fulfilment_group_id BIGINT NOT NULL REFERENCES manacommunity.fresh_fulfilment_group(id) ON DELETE CASCADE,
    order_item_id BIGINT NOT NULL REFERENCES manacommunity.commerce_order_item(id) ON DELETE CASCADE,
    vendor_product_id BIGINT REFERENCES manacommunity.vendor_product(id),
    batch_id BIGINT REFERENCES manacommunity.food_production_batch(id),
    requested_quantity NUMERIC(12, 3) NOT NULL,
    picked_quantity NUMERIC(12, 3),
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'UNIT',
    unit_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    billed_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    is_weight_adjusted BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fresh_fulfilment_item_group ON manacommunity.fresh_fulfilment_item(fulfilment_group_id);
CREATE INDEX IF NOT EXISTS idx_fresh_fulfilment_item_order_item ON manacommunity.fresh_fulfilment_item(order_item_id);
CREATE INDEX IF NOT EXISTS idx_fresh_fulfilment_item_product ON manacommunity.fresh_fulfilment_item(vendor_product_id);

-- 3. Fresh Weight Adjustment Log
CREATE TABLE IF NOT EXISTS manacommunity.fresh_weight_adjustment_log (
    id BIGSERIAL PRIMARY KEY,
    adjustment_reference VARCHAR(60) UNIQUE NOT NULL,
    fulfilment_item_id BIGINT NOT NULL REFERENCES manacommunity.fresh_fulfilment_item(id) ON DELETE CASCADE,
    order_id BIGINT NOT NULL REFERENCES manacommunity.commerce_order(id),
    customer_id BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    requested_weight_g NUMERIC(12, 3) NOT NULL,
    actual_weight_g NUMERIC(12, 3) NOT NULL,
    weight_difference_g NUMERIC(12, 3) NOT NULL,
    weight_difference_pct NUMERIC(6, 2) NOT NULL,
    max_tolerance_pct NUMERIC(6, 2) NOT NULL DEFAULT 15.00,
    original_amount NUMERIC(12, 2) NOT NULL,
    adjusted_amount NUMERIC(12, 2) NOT NULL,
    refund_or_charge_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    adjustment_status VARCHAR(50) NOT NULL DEFAULT 'AUTO_ADJUSTED',
    reason VARCHAR(255),
    weighed_by_user_id BIGINT REFERENCES manacommunity.app_user(id),
    weighed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fresh_weight_adj_item ON manacommunity.fresh_weight_adjustment_log(fulfilment_item_id);
CREATE INDEX IF NOT EXISTS idx_fresh_weight_adj_order ON manacommunity.fresh_weight_adjustment_log(order_id);
CREATE INDEX IF NOT EXISTS idx_fresh_weight_adj_customer ON manacommunity.fresh_weight_adjustment_log(customer_id);

-- 4. Fresh Product Recall
CREATE TABLE IF NOT EXISTS manacommunity.fresh_product_recall (
    id BIGSERIAL PRIMARY KEY,
    recall_reference VARCHAR(50) UNIQUE NOT NULL,
    batch_id BIGINT REFERENCES manacommunity.food_production_batch(id),
    vendor_product_id BIGINT NOT NULL REFERENCES manacommunity.vendor_product(id),
    severity VARCHAR(30) NOT NULL DEFAULT 'MAJOR',
    reason VARCHAR(255) NOT NULL,
    detailed_root_cause TEXT,
    initiated_by_user_id BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    initiated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
    affected_orders_count INT NOT NULL DEFAULT 0,
    affected_customers_count INT NOT NULL DEFAULT 0,
    total_units_recalled NUMERIC(12, 3) NOT NULL DEFAULT 0.00,
    total_refund_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fresh_recall_batch ON manacommunity.fresh_product_recall(batch_id);
CREATE INDEX IF NOT EXISTS idx_fresh_recall_product ON manacommunity.fresh_product_recall(vendor_product_id);
CREATE INDEX IF NOT EXISTS idx_fresh_recall_status ON manacommunity.fresh_product_recall(status);

-- 5. Fresh Product Recall Order Mapping
CREATE TABLE IF NOT EXISTS manacommunity.fresh_product_recall_order (
    id BIGSERIAL PRIMARY KEY,
    recall_id BIGINT NOT NULL REFERENCES manacommunity.fresh_product_recall(id) ON DELETE CASCADE,
    order_id BIGINT NOT NULL REFERENCES manacommunity.commerce_order(id),
    customer_id BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    fulfilment_item_id BIGINT REFERENCES manacommunity.fresh_fulfilment_item(id),
    quantity NUMERIC(12, 3) NOT NULL,
    refund_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    customer_notified BOOLEAN NOT NULL DEFAULT FALSE,
    refund_processed BOOLEAN NOT NULL DEFAULT FALSE,
    notification_sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fresh_recall_order_recall ON manacommunity.fresh_product_recall_order(recall_id);
CREATE INDEX IF NOT EXISTS idx_fresh_recall_order_order ON manacommunity.fresh_product_recall_order(order_id);
CREATE INDEX IF NOT EXISTS idx_fresh_recall_order_customer ON manacommunity.fresh_product_recall_order(customer_id);

-- 6. Fresh Product Wastage Analysis
CREATE TABLE IF NOT EXISTS manacommunity.fresh_product_wastage (
    id BIGSERIAL PRIMARY KEY,
    wastage_reference VARCHAR(50) UNIQUE NOT NULL,
    vendor_product_id BIGINT NOT NULL REFERENCES manacommunity.vendor_product(id),
    batch_id BIGINT REFERENCES manacommunity.food_production_batch(id),
    fulfilment_location_id BIGINT REFERENCES manacommunity.fresh_fulfilment_location(id),
    quantity NUMERIC(12, 3) NOT NULL,
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'KG',
    cost_price_per_unit NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    total_loss_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    wastage_category VARCHAR(50) NOT NULL,
    notes TEXT,
    disposal_method VARCHAR(100),
    recorded_by_user_id BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fresh_wastage_product ON manacommunity.fresh_product_wastage(vendor_product_id);
CREATE INDEX IF NOT EXISTS idx_fresh_wastage_batch ON manacommunity.fresh_product_wastage(batch_id);
CREATE INDEX IF NOT EXISTS idx_fresh_wastage_location ON manacommunity.fresh_product_wastage(fulfilment_location_id);
CREATE INDEX IF NOT EXISTS idx_fresh_wastage_category ON manacommunity.fresh_product_wastage(wastage_category);

-- 7. Immutable Stock Movement Ledger
CREATE TABLE IF NOT EXISTS manacommunity.fresh_stock_movement (
    id BIGSERIAL PRIMARY KEY,
    vendor_product_id BIGINT NOT NULL REFERENCES manacommunity.vendor_product(id),
    batch_id BIGINT REFERENCES manacommunity.food_production_batch(id),
    fulfilment_location_id BIGINT REFERENCES manacommunity.fresh_fulfilment_location(id),
    movement_type VARCHAR(50) NOT NULL,
    quantity NUMERIC(12, 3) NOT NULL,
    balance_after NUMERIC(12, 3) NOT NULL,
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'KG',
    reference_type VARCHAR(50),
    reference_id VARCHAR(100),
    created_by_user_id BIGINT REFERENCES manacommunity.app_user(id),
    notes VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fresh_stock_movement_product ON manacommunity.fresh_stock_movement(vendor_product_id);
CREATE INDEX IF NOT EXISTS idx_fresh_stock_movement_batch ON manacommunity.fresh_stock_movement(batch_id);
CREATE INDEX IF NOT EXISTS idx_fresh_stock_movement_location ON manacommunity.fresh_stock_movement(fulfilment_location_id);
CREATE INDEX IF NOT EXISTS idx_fresh_stock_movement_type ON manacommunity.fresh_stock_movement(movement_type);
