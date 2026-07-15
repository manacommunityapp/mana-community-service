-- FILE: db/sql/v1.0.0/26_inventory_management_phase2.sql
-- ═══════════════════════════════════════════════════════════════════════════
-- INVENTORY MODULE — Phase 2: Picking Types, Pickings, Move Lines, Stock Quants
-- ═══════════════════════════════════════════════════════════════════════════

-- Operation types: INCOMING (receipts), OUTGOING (deliveries), INTERNAL (transfers)
CREATE TABLE IF NOT EXISTS inventory_picking_type (
    id              BIGSERIAL    PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(20)  NOT NULL,
    sequence_prefix VARCHAR(20)  NOT NULL,
    warehouse_id    BIGINT       NOT NULL REFERENCES inventory_warehouse(id) ON DELETE CASCADE,
    default_location_src_id  BIGINT REFERENCES inventory_location(id),
    default_location_dest_id BIGINT REFERENCES inventory_location(id),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_inv_pt_warehouse ON inventory_picking_type(warehouse_id);

-- Picking = a transfer order (receipt, delivery, or internal move)
CREATE TABLE IF NOT EXISTS inventory_picking (
    id              BIGSERIAL    PRIMARY KEY,
    name            VARCHAR(50)  NOT NULL,
    state           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    picking_type_id BIGINT       NOT NULL REFERENCES inventory_picking_type(id),
    location_id     BIGINT       NOT NULL REFERENCES inventory_location(id),
    location_dest_id BIGINT      NOT NULL REFERENCES inventory_location(id),
    partner_id      BIGINT,
    scheduled_date  TIMESTAMP,
    date_done       TIMESTAMP,
    origin          VARCHAR(200),
    backorder_id    BIGINT       REFERENCES inventory_picking(id),
    community_id    BIGINT       REFERENCES community(id),
    created_by      BIGINT       REFERENCES app_user(id),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_inv_picking_type    ON inventory_picking(picking_type_id);
CREATE INDEX IF NOT EXISTS idx_inv_picking_state   ON inventory_picking(state);
CREATE INDEX IF NOT EXISTS idx_inv_picking_community ON inventory_picking(community_id);

-- Move lines = individual product lines within a picking
CREATE TABLE IF NOT EXISTS inventory_move_line (
    id              BIGSERIAL       PRIMARY KEY,
    picking_id      BIGINT          NOT NULL REFERENCES inventory_picking(id) ON DELETE CASCADE,
    product_id      BIGINT          NOT NULL REFERENCES inventory_product(id),
    product_qty     NUMERIC(12,2)   NOT NULL DEFAULT 0,
    qty_done        NUMERIC(12,2)   NOT NULL DEFAULT 0,
    location_id     BIGINT          NOT NULL REFERENCES inventory_location(id),
    location_dest_id BIGINT         NOT NULL REFERENCES inventory_location(id),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_inv_ml_picking ON inventory_move_line(picking_id);
CREATE INDEX IF NOT EXISTS idx_inv_ml_product ON inventory_move_line(product_id);

-- Stock quant = the ledger. Single source of truth for "how much of X is at location Y"
CREATE TABLE IF NOT EXISTS inventory_stock_quant (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL REFERENCES inventory_product(id),
    location_id     BIGINT          NOT NULL REFERENCES inventory_location(id),
    quantity        NUMERIC(12,2)   NOT NULL DEFAULT 0,
    reserved_qty    NUMERIC(12,2)   NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    UNIQUE (product_id, location_id)
);

CREATE INDEX IF NOT EXISTS idx_inv_sq_product  ON inventory_stock_quant(product_id);
CREATE INDEX IF NOT EXISTS idx_inv_sq_location ON inventory_stock_quant(location_id);

-- Picking sequence counter (one row per picking type, atomically incremented)
CREATE TABLE IF NOT EXISTS inventory_sequence (
    id              BIGSERIAL    PRIMARY KEY,
    prefix          VARCHAR(20)  NOT NULL UNIQUE,
    next_val        BIGINT       NOT NULL DEFAULT 1
);

-- ═══════════════════════════════════════════════════════════════════════════
-- Seed: default picking types + sequences for the Main Warehouse
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO inventory_picking_type (name, code, sequence_prefix, warehouse_id, default_location_src_id, default_location_dest_id)
VALUES
    ('Receipts',    'INCOMING', 'WH/IN/',  (SELECT id FROM inventory_warehouse WHERE code = 'WH' LIMIT 1),
        (SELECT id FROM inventory_location WHERE complete_name = 'Partner/Vendor' LIMIT 1),
        (SELECT id FROM inventory_location WHERE complete_name = 'WH/Stock' LIMIT 1)),
    ('Deliveries',  'OUTGOING', 'WH/OUT/', (SELECT id FROM inventory_warehouse WHERE code = 'WH' LIMIT 1),
        (SELECT id FROM inventory_location WHERE complete_name = 'WH/Stock' LIMIT 1),
        (SELECT id FROM inventory_location WHERE complete_name = 'Partner/Customer' LIMIT 1)),
    ('Internal',    'INTERNAL', 'WH/INT/', (SELECT id FROM inventory_warehouse WHERE code = 'WH' LIMIT 1),
        (SELECT id FROM inventory_location WHERE complete_name = 'WH/Stock' LIMIT 1),
        (SELECT id FROM inventory_location WHERE complete_name = 'WH/Stock' LIMIT 1))
ON CONFLICT DO NOTHING;

INSERT INTO inventory_sequence (prefix, next_val) VALUES
    ('WH/IN/',  1),
    ('WH/OUT/', 1),
    ('WH/INT/', 1)
ON CONFLICT DO NOTHING;
