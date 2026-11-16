-- V266: Fresh Demand Aggregation, Delivery Dispatch Routing & Financial Settlements

-- 1. Central Demand Aggregation: Vendor Purchase Orders
CREATE TABLE IF NOT EXISTS manacommunity.fresh_purchase_order (
    id                      BIGSERIAL PRIMARY KEY,
    po_number               VARCHAR(60) NOT NULL UNIQUE,
    vms_vendor_id           BIGINT NOT NULL REFERENCES manacommunity.vms_vendors(id) ON DELETE CASCADE,
    target_delivery_date    DATE NOT NULL,
    status                  VARCHAR(40) NOT NULL DEFAULT 'DRAFT', -- DRAFT, SENT_TO_VENDOR, ACCEPTED, IN_FULFILMENT, COMPLETED, CANCELLED
    total_amount            NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    notes                   VARCHAR(500),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_fresh_po_vendor ON manacommunity.fresh_purchase_order(vms_vendor_id);
CREATE INDEX IF NOT EXISTS idx_fresh_po_date ON manacommunity.fresh_purchase_order(target_delivery_date);

-- 2. Purchase Order Items (Demand breakdown by source)
CREATE TABLE IF NOT EXISTS manacommunity.fresh_purchase_order_item (
    id                      BIGSERIAL PRIMARY KEY,
    purchase_order_id       BIGINT NOT NULL REFERENCES manacommunity.fresh_purchase_order(id) ON DELETE CASCADE,
    vendor_product_id       BIGINT NOT NULL REFERENCES manacommunity.vendor_product(id) ON DELETE CASCADE,
    quantity                NUMERIC(10,2) NOT NULL,
    unit_cost               NUMERIC(10,2) NOT NULL,
    total_cost              NUMERIC(12,2) NOT NULL,
    demand_source           VARCHAR(40) NOT NULL DEFAULT 'RETAIL_ORDER', -- SUBSCRIPTION, GROUP_BUY, RETAIL_ORDER, WELLNESS_PLAN
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_fresh_po_item_po ON manacommunity.fresh_purchase_order_item(purchase_order_id);

-- 3. Community Delivery Dispatch Batches
CREATE TABLE IF NOT EXISTS manacommunity.fresh_delivery_batch (
    id                      BIGSERIAL PRIMARY KEY,
    batch_code              VARCHAR(60) NOT NULL UNIQUE,
    community_id            BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    delivery_date           DATE NOT NULL,
    handling_class          VARCHAR(40) NOT NULL DEFAULT 'CHILLED_COLD_CHAIN', -- AMBIENT, CHILLED_COLD_CHAIN, FROZEN
    assigned_runner_id      BIGINT REFERENCES manacommunity.app_user(id),
    status                  VARCHAR(40) NOT NULL DEFAULT 'PLANNED', -- PLANNED, LOADED, IN_TRANSIT, DELIVERED, COMPLETED
    dispatched_at           TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_fresh_deliv_comm ON manacommunity.fresh_delivery_batch(community_id, delivery_date);

-- 4. Community Delivery Items (Door-to-door drop sequence)
CREATE TABLE IF NOT EXISTS manacommunity.fresh_delivery_item (
    id                      BIGSERIAL PRIMARY KEY,
    delivery_batch_id       BIGINT NOT NULL REFERENCES manacommunity.fresh_delivery_batch(id) ON DELETE CASCADE,
    commerce_order_id       BIGINT REFERENCES manacommunity.commerce_order(id),
    customer_name           VARCHAR(120),
    tower                   VARCHAR(40),
    flat_no                 VARCHAR(30),
    sort_sequence           INT NOT NULL DEFAULT 0,
    handover_status         VARCHAR(40) NOT NULL DEFAULT 'PENDING', -- PENDING, DELIVERED, RESCHEDULED, RETURNED
    delivered_at            TIMESTAMPTZ,
    proof_image_url         TEXT,
    notes                   VARCHAR(255),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_fresh_deliv_item_batch ON manacommunity.fresh_delivery_item(delivery_batch_id);

-- 5. Vendor Financial Settlements
CREATE TABLE IF NOT EXISTS manacommunity.fresh_vendor_settlement (
    id                      BIGSERIAL PRIMARY KEY,
    settlement_reference    VARCHAR(80) NOT NULL UNIQUE,
    vms_vendor_id           BIGINT NOT NULL REFERENCES manacommunity.vms_vendors(id) ON DELETE CASCADE,
    settlement_period_start DATE NOT NULL,
    settlement_period_end   DATE NOT NULL,
    gross_sales_amount      NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    mana_commission_amount  NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    delivery_fees_collected NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    gateway_fees_deducted   NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    net_payable_amount      NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    status                  VARCHAR(40) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, PROCESSED, FAILED
    payout_utr_reference    VARCHAR(100),
    processed_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_fresh_settle_vendor ON manacommunity.fresh_vendor_settlement(vms_vendor_id);