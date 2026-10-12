-- Phase 2: Product/Catalog
-- Adds product type, community pricing tiers, and product categories.

-- 1. Commerce product categories (hierarchical)
CREATE TABLE commerce_product_category (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    slug          VARCHAR(120) NOT NULL UNIQUE,
    icon          VARCHAR(100),
    description   VARCHAR(500),
    parent_id     BIGINT REFERENCES commerce_product_category(id) ON DELETE SET NULL,
    community_id  BIGINT REFERENCES community(id) ON DELETE CASCADE,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cpc_community   ON commerce_product_category (community_id);
CREATE INDEX idx_cpc_parent      ON commerce_product_category (parent_id) WHERE parent_id IS NOT NULL;
CREATE INDEX idx_cpc_active      ON commerce_product_category (active) WHERE active = TRUE;

-- 2. Extend vendor_product with type, community flags, physical attributes
ALTER TABLE vendor_product
    ADD COLUMN IF NOT EXISTS product_type         VARCHAR(30) NOT NULL DEFAULT 'PHYSICAL_PRODUCT',
    ADD COLUMN IF NOT EXISTS community_only        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS additional_images     TEXT,
    ADD COLUMN IF NOT EXISTS weight_grams          INTEGER,
    ADD COLUMN IF NOT EXISTS dimensions_cm         VARCHAR(60),
    ADD COLUMN IF NOT EXISTS commerce_category_id  BIGINT REFERENCES commerce_product_category(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS product_code          VARCHAR(60),
    ADD COLUMN IF NOT EXISTS tags                  TEXT;

ALTER TABLE vendor_product
    ADD CONSTRAINT chk_product_type CHECK (product_type IN (
        'PHYSICAL_PRODUCT','DIGITAL_PRODUCT','SERVICE','FOOD','BOOKING','SUBSCRIPTION'
    ));

CREATE INDEX IF NOT EXISTS idx_vp_product_type ON vendor_product (product_type);
CREATE INDEX IF NOT EXISTS idx_vp_community_only ON vendor_product (community_only) WHERE community_only = TRUE;
CREATE INDEX IF NOT EXISTS idx_vp_category_id ON vendor_product (commerce_category_id) WHERE commerce_category_id IS NOT NULL;

-- 3. Extend vendor_product_variant with community pricing tiers
-- New price columns use NUMERIC(19,4) per the master prompt's financial precision standard.
ALTER TABLE vendor_product_variant
    ADD COLUMN IF NOT EXISTS member_price           NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS bulk_min_qty           INTEGER DEFAULT 5,
    ADD COLUMN IF NOT EXISTS bulk_price             NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS promotional_price      NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS promotional_end_date   DATE,
    ADD COLUMN IF NOT EXISTS festival_tag           VARCHAR(60),
    ADD COLUMN IF NOT EXISTS festival_price         NUMERIC(19,4);

-- Ensure mrp / vendorCost / defaultCommunityPrice are always positive
ALTER TABLE vendor_product_variant
    ADD CONSTRAINT chk_vpv_mrp_positive    CHECK (mrp >= 0),
    ADD CONSTRAINT chk_vpv_cost_positive   CHECK (vendor_cost >= 0);
