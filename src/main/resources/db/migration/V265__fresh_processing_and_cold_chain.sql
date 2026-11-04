-- V265: Fresh Processing Recipes, Production Batches, Quality Checklists & Cold Chain Logs

-- 1. Processing Recipes (Bill of Materials & specs for Fresh-Cut, Batters, Nutrition Boxes, etc.)
CREATE TABLE IF NOT EXISTS manacommunity.fresh_processing_recipe (
    id                          BIGSERIAL PRIMARY KEY,
    vms_vendor_id               BIGINT NOT NULL REFERENCES manacommunity.vms_vendors(id) ON DELETE CASCADE,
    recipe_code                 VARCHAR(60) NOT NULL UNIQUE,
    recipe_name                 VARCHAR(150) NOT NULL,
    output_product_id           BIGINT NOT NULL REFERENCES manacommunity.vendor_product(id) ON DELETE CASCADE,
    target_output_unit          VARCHAR(30) NOT NULL,  -- KG, BOX, PACK, LITRE
    expected_yield_percent      NUMERIC(5,2) NOT NULL DEFAULT 100.00,
    storage_temp_min_celsius    NUMERIC(4,1) NOT NULL DEFAULT 2.0,
    storage_temp_max_celsius    NUMERIC(4,1) NOT NULL DEFAULT 4.0,
    shelf_life_hours            INT NOT NULL DEFAULT 24,
    instructions                TEXT,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_fresh_recipe_vendor ON manacommunity.fresh_processing_recipe(vms_vendor_id);

-- 2. Production Batches
CREATE TABLE IF NOT EXISTS manacommunity.fresh_processing_batch (
    id                          BIGSERIAL PRIMARY KEY,
    batch_number                VARCHAR(80) NOT NULL UNIQUE,
    recipe_id                   BIGINT REFERENCES manacommunity.fresh_processing_recipe(id),
    vms_vendor_id               BIGINT NOT NULL REFERENCES manacommunity.vms_vendors(id) ON DELETE CASCADE,
    vendor_product_id           BIGINT NOT NULL REFERENCES manacommunity.vendor_product(id) ON DELETE CASCADE,
    production_date             DATE NOT NULL,
    raw_material_input_qty      NUMERIC(10,2) NOT NULL,
    actual_output_qty           NUMERIC(10,2) NOT NULL,
    actual_yield_percent        NUMERIC(5,2) NOT NULL,
    wastage_qty                 NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    wastage_reason              VARCHAR(255),
    status                      VARCHAR(40) NOT NULL DEFAULT 'IN_PRODUCTION', -- IN_PRODUCTION, FERMENTING, QUALITY_INSPECTION, RELEASED, QUARANTINED, REJECTED
    storage_temp_celsius        NUMERIC(4,1),
    expiry_timestamp            TIMESTAMPTZ NOT NULL,
    notes                       TEXT,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                     BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_fresh_batch_vendor ON manacommunity.fresh_processing_batch(vms_vendor_id);
CREATE INDEX IF NOT EXISTS idx_fresh_batch_status ON manacommunity.fresh_processing_batch(status);

-- 3. Quality Inspection Checklists
CREATE TABLE IF NOT EXISTS manacommunity.fresh_batch_quality_check (
    id                          BIGSERIAL PRIMARY KEY,
    batch_id                    BIGINT NOT NULL REFERENCES manacommunity.fresh_processing_batch(id) ON DELETE CASCADE,
    check_name                  VARCHAR(100) NOT NULL, -- SENSORY_VISUAL, PH_ACIDITY, TEMPERATURE, PACKAGING_SEAL, HYGIENE_SWAB
    measured_value              VARCHAR(100) NOT NULL,
    is_passed                   BOOLEAN NOT NULL,
    checked_by_user_id          BIGINT NOT NULL REFERENCES manacommunity.app_users(id),
    checked_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    notes                       VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_fresh_qc_batch ON manacommunity.fresh_batch_quality_check(batch_id);

-- 4. Batch Release Authorization Gate
CREATE TABLE IF NOT EXISTS manacommunity.fresh_batch_release (
    id                          BIGSERIAL PRIMARY KEY,
    batch_id                    BIGINT NOT NULL UNIQUE REFERENCES manacommunity.fresh_processing_batch(id) ON DELETE CASCADE,
    released_by_user_id         BIGINT NOT NULL REFERENCES manacommunity.app_users(id),
    release_decision            VARCHAR(30) NOT NULL, -- APPROVED, REJECTED, QUARANTINED
    temperature_verified_celsius NUMERIC(4,1) NOT NULL,
    released_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    remarks                     VARCHAR(500)
);

-- 5. Continuous / Spot Cold Chain Temperature Logs
CREATE TABLE IF NOT EXISTS manacommunity.fresh_batch_temperature_log (
    id                          BIGSERIAL PRIMARY KEY,
    batch_id                    BIGINT NOT NULL REFERENCES manacommunity.fresh_processing_batch(id) ON DELETE CASCADE,
    logged_temp_celsius         NUMERIC(4,1) NOT NULL,
    location_tag                VARCHAR(100) NOT NULL, -- COLD_ROOM_1, TRANSIT_CRATE, PACKING_BENCH, DISPATCH_DOCK
    logged_by_user_id           BIGINT REFERENCES manacommunity.app_users(id),
    logged_at                   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_fresh_temp_log_batch ON manacommunity.fresh_batch_temperature_log(batch_id);