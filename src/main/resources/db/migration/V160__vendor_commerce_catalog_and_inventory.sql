-- V160: Vendor Product Catalog, Variants, and Multi-State Inventory Ledger

CREATE TABLE IF NOT EXISTS manacommunity.vendor_product (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    vendor_user_id BIGINT NOT NULL REFERENCES manacommunity.app_user(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(80) NOT NULL,
    sub_category VARCHAR(80),
    brand VARCHAR(120),
    description TEXT,
    hsn_code VARCHAR(30),
    gst_rate NUMERIC(5, 2) DEFAULT 5.00,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    image_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_vendor_prod_user ON manacommunity.vendor_product(vendor_user_id);
CREATE INDEX IF NOT EXISTS idx_vendor_prod_comm ON manacommunity.vendor_product(community_id);

CREATE TABLE IF NOT EXISTS manacommunity.vendor_product_variant (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES manacommunity.vendor_product(id) ON DELETE CASCADE,
    variant_name VARCHAR(120) NOT NULL,
    sku VARCHAR(80) NOT NULL,
    barcode VARCHAR(80),
    pack_size VARCHAR(50) NOT NULL,
    mrp NUMERIC(12, 2) NOT NULL,
    vendor_cost NUMERIC(12, 2) NOT NULL,
    default_community_price NUMERIC(12, 2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_vendor_prod_variant_prod ON manacommunity.vendor_product_variant(product_id);
CREATE UNIQUE INDEX IF NOT EXISTS unq_vendor_variant_sku ON manacommunity.vendor_product_variant(sku);

CREATE TABLE IF NOT EXISTS manacommunity.vendor_product_inventory (
    id BIGSERIAL PRIMARY KEY,
    variant_id BIGINT NOT NULL UNIQUE REFERENCES manacommunity.vendor_product_variant(id) ON DELETE CASCADE,
    available_qty INTEGER NOT NULL DEFAULT 0,
    reserved_qty INTEGER NOT NULL DEFAULT 0,
    committed_qty INTEGER NOT NULL DEFAULT 0,
    allocated_qty INTEGER NOT NULL DEFAULT 0,
    picked_qty INTEGER NOT NULL DEFAULT 0,
    dispatched_qty INTEGER NOT NULL DEFAULT 0,
    delivered_qty INTEGER NOT NULL DEFAULT 0,
    damaged_qty INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS manacommunity.vendor_inventory_batch (
    id BIGSERIAL PRIMARY KEY,
    variant_id BIGINT NOT NULL REFERENCES manacommunity.vendor_product_variant(id) ON DELETE CASCADE,
    batch_number VARCHAR(60) NOT NULL,
    manufacturing_date DATE,
    expiry_date DATE,
    received_qty INTEGER NOT NULL,
    remaining_qty INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vendor_batch_variant ON manacommunity.vendor_inventory_batch(variant_id);
CREATE INDEX IF NOT EXISTS idx_vendor_batch_expiry ON manacommunity.vendor_inventory_batch(expiry_date);
