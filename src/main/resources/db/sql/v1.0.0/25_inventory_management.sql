-- FILE: db/sql/v1.0.0/25_inventory_management.sql
-- ═══════════════════════════════════════════════════════════════════════════
-- INVENTORY MODULE — Phase 1: Product, Warehouse, Location, Category
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS inventory_category (
    id              BIGSERIAL    PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    parent_id       BIGINT       REFERENCES inventory_category(id) ON DELETE SET NULL,
    community_id    BIGINT       REFERENCES community(id),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS inventory_product (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(200)    NOT NULL,
    default_code    VARCHAR(50),
    barcode         VARCHAR(100),
    list_price      NUMERIC(12,2)   NOT NULL DEFAULT 0,
    standard_price  NUMERIC(12,2)   NOT NULL DEFAULT 0,
    type            VARCHAR(20)     NOT NULL DEFAULT 'STORABLE',
    tracking        VARCHAR(10)     NOT NULL DEFAULT 'NONE',
    category_id     BIGINT          REFERENCES inventory_category(id),
    community_id    BIGINT          REFERENCES community(id),
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_inv_product_category  ON inventory_product(category_id);
CREATE INDEX IF NOT EXISTS idx_inv_product_community ON inventory_product(community_id);
CREATE INDEX IF NOT EXISTS idx_inv_product_code      ON inventory_product(default_code);
CREATE INDEX IF NOT EXISTS idx_inv_product_barcode   ON inventory_product(barcode);

CREATE TABLE IF NOT EXISTS inventory_warehouse (
    id              BIGSERIAL    PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(10)  NOT NULL,
    reception_steps VARCHAR(20)  NOT NULL DEFAULT 'ONE_STEP',
    delivery_steps  VARCHAR(20)  NOT NULL DEFAULT 'ONE_STEP',
    community_id    BIGINT       REFERENCES community(id),
    is_active       BOOLEAN      NOT NULL DEFAULT true,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_inv_warehouse_community ON inventory_warehouse(community_id);

CREATE TABLE IF NOT EXISTS inventory_location (
    id              BIGSERIAL    PRIMARY KEY,
    complete_name   VARCHAR(200) NOT NULL,
    usage           VARCHAR(20)  NOT NULL DEFAULT 'INTERNAL',
    barcode         VARCHAR(100),
    warehouse_id    BIGINT       REFERENCES inventory_warehouse(id) ON DELETE CASCADE,
    is_active       BOOLEAN      NOT NULL DEFAULT true,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_inv_location_warehouse ON inventory_location(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inv_location_usage     ON inventory_location(usage);

-- ═══════════════════════════════════════════════════════════════════════════
-- Seed: default warehouse + standard locations + categories
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO inventory_category (name, parent_id) VALUES
    ('All / Saleable', NULL),
    ('Raw Materials', NULL),
    ('Finished Goods', NULL),
    ('Consumables', NULL);

INSERT INTO inventory_warehouse (name, code) VALUES
    ('Main Warehouse', 'WH');

INSERT INTO inventory_location (complete_name, usage, warehouse_id) VALUES
    ('WH/Stock',     'INTERNAL',  (SELECT id FROM inventory_warehouse WHERE code = 'WH' LIMIT 1)),
    ('WH/Input',     'INTERNAL',  (SELECT id FROM inventory_warehouse WHERE code = 'WH' LIMIT 1)),
    ('WH/Output',    'INTERNAL',  (SELECT id FROM inventory_warehouse WHERE code = 'WH' LIMIT 1)),
    ('WH/Quality',   'INTERNAL',  (SELECT id FROM inventory_warehouse WHERE code = 'WH' LIMIT 1)),
    ('Partner/Vendor','VENDOR',   (SELECT id FROM inventory_warehouse WHERE code = 'WH' LIMIT 1)),
    ('Partner/Customer','CUSTOMER',(SELECT id FROM inventory_warehouse WHERE code = 'WH' LIMIT 1)),
    ('Virtual/Scrap','SCRAP',     (SELECT id FROM inventory_warehouse WHERE code = 'WH' LIMIT 1)),
    ('Virtual/Inventory Adjustment','INVENTORY',(SELECT id FROM inventory_warehouse WHERE code = 'WH' LIMIT 1));
