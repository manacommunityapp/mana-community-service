-- FILE: db/sql/v1.0.0/27_inventory_management_phase3.sql
-- ═══════════════════════════════════════════════════════════════════════════
-- INVENTORY MODULE — Phase 3: Scrap Records, Lot/Serial Tracking
-- ═══════════════════════════════════════════════════════════════════════════

-- Lot / Serial numbers for tracked products
CREATE TABLE IF NOT EXISTS inventory_lot (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    product_id      BIGINT          NOT NULL REFERENCES inventory_product(id),
    expiration_date DATE,
    notes           TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    UNIQUE (name, product_id)
);

CREATE INDEX IF NOT EXISTS idx_inv_lot_product ON inventory_lot(product_id);

-- Scrap records — permanent audit trail of every scrap operation
CREATE TABLE IF NOT EXISTS inventory_scrap (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL REFERENCES inventory_product(id),
    lot_id          BIGINT          REFERENCES inventory_lot(id),
    location_id     BIGINT          NOT NULL REFERENCES inventory_location(id),
    scrap_location_id BIGINT        NOT NULL REFERENCES inventory_location(id),
    quantity        NUMERIC(12,2)   NOT NULL,
    reason          VARCHAR(500),
    state           VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    scrapped_by     BIGINT          REFERENCES app_user(id),
    scrapped_at     TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_inv_scrap_product  ON inventory_scrap(product_id);
CREATE INDEX IF NOT EXISTS idx_inv_scrap_state    ON inventory_scrap(state);

-- Add lot_id FK to move lines for lot/serial-tracked moves
ALTER TABLE inventory_move_line
    ADD COLUMN IF NOT EXISTS lot_id BIGINT REFERENCES inventory_lot(id);

-- Add lot_id FK to stock quants for lot-level stock tracking
ALTER TABLE inventory_stock_quant
    ADD COLUMN IF NOT EXISTS lot_id BIGINT REFERENCES inventory_lot(id);

-- New unique: one quant row per product + location + lot
-- (lot_id = NULL for untracked products, which still works with UNIQUE)
DROP INDEX IF EXISTS uq_quant_product_location;
ALTER TABLE inventory_stock_quant
    DROP CONSTRAINT IF EXISTS uq_quant_product_location;
DROP INDEX IF EXISTS uq_quant_product_location_lot;
CREATE UNIQUE INDEX IF NOT EXISTS uq_quant_product_location_lot
    ON inventory_stock_quant(product_id, location_id, COALESCE(lot_id, -1));

-- Inventory valuation method on product (for future costing)
ALTER TABLE inventory_product
    ADD COLUMN IF NOT EXISTS valuation_method VARCHAR(20) DEFAULT 'STANDARD';
