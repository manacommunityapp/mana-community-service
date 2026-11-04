-- V264: Fresh Product Attributes, Nutrition, Allergens & Pricing Margin
-- Extends vendor_product with fresh-specific catalog enrichment data

-- Dynamic per-product attributes (fermentation hours, grading, weight bands, etc.)
CREATE TABLE IF NOT EXISTS manacommunity.fresh_product_attribute (
    id                  BIGSERIAL PRIMARY KEY,
    vendor_product_id   BIGINT NOT NULL REFERENCES manacommunity.vendor_product(id) ON DELETE CASCADE,
    attribute_key       VARCHAR(80)  NOT NULL,  -- e.g. FERMENTATION_HOURS, GRADE, CUT_TYPE
    attribute_value     VARCHAR(200) NOT NULL,
    unit                VARCHAR(30),             -- e.g. hours, kg, cm
    display_order       INT          NOT NULL DEFAULT 0,
    is_searchable       BOOLEAN      NOT NULL DEFAULT false,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_fresh_product_attr_product ON manacommunity.fresh_product_attribute(vendor_product_id);

-- Full nutrition panel per product (macro + micro nutrients)
CREATE TABLE IF NOT EXISTS manacommunity.fresh_product_nutrition (
    id                      BIGSERIAL PRIMARY KEY,
    vendor_product_id       BIGINT NOT NULL UNIQUE REFERENCES manacommunity.vendor_product(id) ON DELETE CASCADE,
    serving_size_grams      NUMERIC(8,2),
    calories_kcal           NUMERIC(8,2),
    protein_g               NUMERIC(8,2),
    carbohydrates_g         NUMERIC(8,2),
    fat_g                   NUMERIC(8,2),
    saturated_fat_g         NUMERIC(8,2),
    fibre_g                 NUMERIC(8,2),
    sodium_mg               NUMERIC(8,2),
    sugar_g                 NUMERIC(8,2),
    -- Micronutrients stored as JSONB for flexibility
    micronutrients          JSONB,
    -- Dietitian certification
    dietitian_certified     BOOLEAN     NOT NULL DEFAULT false,
    dietitian_name          VARCHAR(120),
    certification_number    VARCHAR(60),
    certified_on            DATE,
    -- FSSAI label compliance
    fssai_label_compliant   BOOLEAN     NOT NULL DEFAULT false,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Allergen declarations per product
CREATE TABLE IF NOT EXISTS manacommunity.fresh_product_allergen (
    id                  BIGSERIAL PRIMARY KEY,
    vendor_product_id   BIGINT      NOT NULL REFERENCES manacommunity.vendor_product(id) ON DELETE CASCADE,
    allergen_name       VARCHAR(80) NOT NULL,  -- e.g. GLUTEN, DAIRY, NUTS, SOY, EGG
    severity            VARCHAR(20) NOT NULL DEFAULT 'CONTAINS',  -- CONTAINS | MAY_CONTAIN | FREE_FROM
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_fresh_allergen_product ON manacommunity.fresh_product_allergen(vendor_product_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_fresh_allergen ON manacommunity.fresh_product_allergen(vendor_product_id, allergen_name);

-- Pricing margin simulator records – vendor cost, Mana commission, delivery, tax → resident price
CREATE TABLE IF NOT EXISTS manacommunity.fresh_product_pricing (
    id                      BIGSERIAL PRIMARY KEY,
    vendor_product_id       BIGINT      NOT NULL REFERENCES manacommunity.vendor_product(id) ON DELETE CASCADE,
    effective_from          DATE        NOT NULL,
    effective_to            DATE,
    vendor_cost_price       NUMERIC(12,2) NOT NULL,
    mana_commission_pct     NUMERIC(5,2)  NOT NULL DEFAULT 10.00,
    delivery_fee            NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    packaging_fee           NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    gst_rate_pct            NUMERIC(5,2)  NOT NULL DEFAULT 5.00,
    -- Computed fields (denormalised for query speed)
    resident_selling_price  NUMERIC(12,2) NOT NULL,
    is_active               BOOLEAN       NOT NULL DEFAULT true,
    created_by_vendor_id    BIGINT        REFERENCES manacommunity.vms_vendors(id),
    admin_approved          BOOLEAN       NOT NULL DEFAULT false,
    admin_approved_by       BIGINT        REFERENCES manacommunity.app_users(id),
    admin_approved_at       TIMESTAMPTZ,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_fresh_pricing_product ON manacommunity.fresh_product_pricing(vendor_product_id, is_active);

-- Fresh-specific catalog profile per product (shelf life, storage, origin, certifications)
CREATE TABLE IF NOT EXISTS manacommunity.fresh_product_catalog_profile (
    id                      BIGSERIAL PRIMARY KEY,
    vendor_product_id       BIGINT      NOT NULL UNIQUE REFERENCES manacommunity.vendor_product(id) ON DELETE CASCADE,
    capability_type         VARCHAR(50) NOT NULL,  -- mirrors FreshVendorCapabilityType
    shelf_life_hours        INT,                   -- 0 for same-day perishables
    storage_instruction     TEXT,
    origin_state            VARCHAR(60),
    origin_district         VARCHAR(60),
    is_organic              BOOLEAN     NOT NULL DEFAULT false,
    organic_cert_number     VARCHAR(80),
    is_pesticide_free       BOOLEAN     NOT NULL DEFAULT false,
    is_antibiotic_free      BOOLEAN     NOT NULL DEFAULT false,  -- for meat/dairy
    is_hormone_free         BOOLEAN     NOT NULL DEFAULT false,
    min_order_qty           NUMERIC(10,3) NOT NULL DEFAULT 1,
    max_order_qty           NUMERIC(10,3),
    order_unit              VARCHAR(30) NOT NULL DEFAULT 'kg',  -- kg, pcs, litre, pack
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);