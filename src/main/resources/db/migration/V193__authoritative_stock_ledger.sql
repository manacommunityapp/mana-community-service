-- FILE: db/migration/V193__authoritative_stock_ledger.sql
-- ═══════════════════════════════════════════════════════════════════════════
-- AUTHORITATIVE STOCK LEDGER & CONCURRENCY CONSTRAINTS
-- ═══════════════════════════════════════════════════════════════════════════

-- 1. Ensure inventory_stock_quant exists with non-negative constraints
DO $$
BEGIN
    -- Check if table exists (from earlier baseline or phase2 sql)
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'inventory_stock_quant') THEN
        CREATE TABLE inventory_stock_quant (
            id              BIGSERIAL       PRIMARY KEY,
            product_id      BIGINT          NOT NULL REFERENCES inventory_product(id),
            location_id     BIGINT          NOT NULL REFERENCES inventory_location(id),
            lot_id          BIGINT          REFERENCES inventory_lot(id),
            quantity        NUMERIC(12,2)   NOT NULL DEFAULT 0,
            reserved_qty    NUMERIC(12,2)   NOT NULL DEFAULT 0,
            updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
        );
        CREATE UNIQUE INDEX IF NOT EXISTS uq_quant_product_location_lot
            ON inventory_stock_quant(product_id, location_id, COALESCE(lot_id, -1));
    END IF;

    -- Add non-negative constraint if not present
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_quant_quantity_non_negative'
    ) THEN
        ALTER TABLE inventory_stock_quant
            ADD CONSTRAINT chk_quant_quantity_non_negative CHECK (quantity >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_quant_reserved_non_negative'
    ) THEN
        ALTER TABLE inventory_stock_quant
            ADD CONSTRAINT chk_quant_reserved_non_negative CHECK (reserved_qty >= 0);
    END IF;
END $$;

-- 2. Create authoritative immutable stock ledger entry table
CREATE TABLE IF NOT EXISTS inventory_stock_ledger_entry (
    id                  BIGSERIAL       PRIMARY KEY,
    community_id        BIGINT          REFERENCES community(id),
    product_id          BIGINT          NOT NULL REFERENCES inventory_product(id),
    location_id         BIGINT          NOT NULL REFERENCES inventory_location(id),
    lot_id              BIGINT          REFERENCES inventory_lot(id),
    movement_type       VARCHAR(40)     NOT NULL, -- RECEIPT, DELIVERY, INTERNAL_TRANSFER, ADJUSTMENT, SCRAP, COMMERCE_FULFILLMENT, RETURN, OPENING_BALANCE
    reference_type      VARCHAR(40),              -- PICKING, PURCHASE_ORDER, GOODS_RECEIPT, COMMERCE_ORDER, MANUAL_ADJUSTMENT
    reference_id        VARCHAR(100),
    quantity_change     NUMERIC(12,2)   NOT NULL, -- positive for inward, negative for outward
    resulting_balance   NUMERIC(12,2)   NOT NULL, -- running balance at that location post-movement
    unit_cost           NUMERIC(12,2)   NOT NULL DEFAULT 0,
    total_value         NUMERIC(12,2)   NOT NULL DEFAULT 0,
    notes               VARCHAR(500),
    created_by          BIGINT          REFERENCES app_user(id),
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_inv_ledger_prod_loc
    ON inventory_stock_ledger_entry(product_id, location_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_inv_ledger_community
    ON inventory_stock_ledger_entry(community_id);

CREATE INDEX IF NOT EXISTS idx_inv_ledger_ref
    ON inventory_stock_ledger_entry(reference_type, reference_id);

CREATE INDEX IF NOT EXISTS idx_inv_ledger_created_at
    ON inventory_stock_ledger_entry(created_at DESC);
