-- V260: Mana Fresh Kitchen & Daily Basket Module
-- Provides tables for fresh food product configurations, vendor production capacity,
-- batch quality tracking with temp gate, breakfast combos and BOM items, and daily basket subscriptions.

-- 1. Fresh Food Product Config
CREATE TABLE IF NOT EXISTS manacommunity.fresh_food_product_config (
    id                    BIGSERIAL PRIMARY KEY,
    product_id            VARCHAR(64)   NOT NULL UNIQUE,
    name                  VARCHAR(200)  NOT NULL,
    description           TEXT,
    category              VARCHAR(50)   NOT NULL,
    execution_mode        VARCHAR(50)   NOT NULL DEFAULT 'DAILY_BASKET',
    unit                  VARCHAR(50)   NOT NULL DEFAULT 'PACK',
    pack_size             VARCHAR(50),
    unit_price            NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    shelf_life_hours      INT           NOT NULL DEFAULT 48,
    storage_temp_min      NUMERIC(4,1)  NOT NULL DEFAULT 2.0,
    storage_temp_max      NUMERIC(4,1)  NOT NULL DEFAULT 4.0,
    min_order_lead_hours  INT           NOT NULL DEFAULT 8,
    image_url             TEXT,
    is_active             BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by            BIGINT,
    updated_by            BIGINT
);

-- 2. Vendor Production Capacity
CREATE TABLE IF NOT EXISTS manacommunity.vendor_production_capacity (
    id                 BIGSERIAL PRIMARY KEY,
    vendor_id          VARCHAR(64)   NOT NULL,
    vendor_name        VARCHAR(150),
    product_id         VARCHAR(64)   NOT NULL,
    product_name       VARCHAR(200),
    production_date    DATE          NOT NULL,
    max_capacity       INT           NOT NULL DEFAULT 0,
    reserved_quantity  INT           NOT NULL DEFAULT 0,
    available_quantity INT           NOT NULL DEFAULT 0,
    is_sold_out        BOOLEAN       NOT NULL DEFAULT FALSE,
    version            BIGINT        NOT NULL DEFAULT 0,
    created_at         TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by         BIGINT,
    updated_by         BIGINT,
    CONSTRAINT uk_vendor_product_date UNIQUE (vendor_id, product_id, production_date)
);

-- 3. Food Production Batch
CREATE TABLE IF NOT EXISTS manacommunity.food_production_batch (
    id                   BIGSERIAL PRIMARY KEY,
    batch_number         VARCHAR(80)   NOT NULL UNIQUE,
    product_id           VARCHAR(64)   NOT NULL,
    product_name         VARCHAR(200)  NOT NULL,
    vendor_id            VARCHAR(64)   NOT NULL,
    production_date      DATE          NOT NULL,
    batch_quantity       INT           NOT NULL DEFAULT 0,
    storage_temp_celsius NUMERIC(4,1),
    batch_status         VARCHAR(40)   NOT NULL DEFAULT 'PLANNED',
    expiry_date_time     TIMESTAMP     NOT NULL,
    released_at          TIMESTAMP,
    released_by          VARCHAR(100),
    notes                TEXT,
    created_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by           BIGINT,
    updated_by           BIGINT
);

-- 4. Breakfast Combo
CREATE TABLE IF NOT EXISTS manacommunity.breakfast_combo (
    id               BIGSERIAL PRIMARY KEY,
    combo_code       VARCHAR(64)   NOT NULL UNIQUE,
    name             VARCHAR(200)  NOT NULL,
    description      TEXT,
    category         VARCHAR(50)   NOT NULL DEFAULT 'BREAKFAST',
    base_price       NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    discounted_price NUMERIC(12,2),
    image_url        TEXT,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by       BIGINT,
    updated_by       BIGINT
);

-- 5. Breakfast Combo Item
CREATE TABLE IF NOT EXISTS manacommunity.breakfast_combo_item (
    id                  BIGSERIAL PRIMARY KEY,
    combo_id            BIGINT        NOT NULL REFERENCES manacommunity.breakfast_combo(id) ON DELETE CASCADE,
    product_id          VARCHAR(64)   NOT NULL,
    product_name        VARCHAR(200)  NOT NULL,
    item_role           VARCHAR(50)   NOT NULL DEFAULT 'MAIN',
    default_quantity    INT           NOT NULL DEFAULT 1,
    unit_price          NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    is_mandatory        BOOLEAN       NOT NULL DEFAULT TRUE,
    is_customizable     BOOLEAN       NOT NULL DEFAULT FALSE,
    allowed_substitutes TEXT,
    sort_order          INT           NOT NULL DEFAULT 0,
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_by          BIGINT
);

-- 6. Daily Basket
CREATE TABLE IF NOT EXISTS manacommunity.daily_basket (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT        NOT NULL REFERENCES manacommunity.app_user(id) ON DELETE CASCADE,
    community_id     BIGINT        NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    is_paused        BOOLEAN       NOT NULL DEFAULT FALSE,
    pause_start_date DATE,
    pause_end_date   DATE,
    delivery_slot    VARCHAR(60)   NOT NULL DEFAULT 'MORNING_06_00_07_00',
    delivery_address TEXT,
    notes            TEXT,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by       BIGINT,
    updated_by       BIGINT,
    CONSTRAINT uk_daily_basket_user_community UNIQUE (user_id, community_id)
);

-- 7. Daily Basket Item
CREATE TABLE IF NOT EXISTS manacommunity.daily_basket_item (
    id                   BIGSERIAL PRIMARY KEY,
    basket_id            BIGINT        NOT NULL REFERENCES manacommunity.daily_basket(id) ON DELETE CASCADE,
    product_id           VARCHAR(64)   NOT NULL,
    product_name         VARCHAR(200)  NOT NULL,
    quantity             INT           NOT NULL DEFAULT 1,
    unit_price           NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    category             VARCHAR(50)   NOT NULL DEFAULT 'BATTER',
    execution_mode       VARCHAR(50)   NOT NULL DEFAULT 'DAILY_BASKET',
    mon                  BOOLEAN       NOT NULL DEFAULT TRUE,
    tue                  BOOLEAN       NOT NULL DEFAULT TRUE,
    wed                  BOOLEAN       NOT NULL DEFAULT TRUE,
    thu                  BOOLEAN       NOT NULL DEFAULT TRUE,
    fri                  BOOLEAN       NOT NULL DEFAULT TRUE,
    sat                  BOOLEAN       NOT NULL DEFAULT TRUE,
    sun                  BOOLEAN       NOT NULL DEFAULT TRUE,
    days_of_week         VARCHAR(80)   NOT NULL DEFAULT 'MON,TUE,WED,THU,FRI,SAT,SUN',
    is_active            BOOLEAN       NOT NULL DEFAULT TRUE,
    special_instructions TEXT,
    created_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by           BIGINT,
    updated_by           BIGINT
);

-- Indexes for performance & query lookups
CREATE INDEX IF NOT EXISTS idx_fresh_food_prod_cat ON manacommunity.fresh_food_product_config(category, is_active);
CREATE INDEX IF NOT EXISTS idx_fresh_food_prod_mode ON manacommunity.fresh_food_product_config(execution_mode, is_active);

CREATE INDEX IF NOT EXISTS idx_vendor_cap_lookup ON manacommunity.vendor_production_capacity(vendor_id, production_date);
CREATE INDEX IF NOT EXISTS idx_vendor_cap_prod_date ON manacommunity.vendor_production_capacity(product_id, production_date);
CREATE INDEX IF NOT EXISTS idx_vendor_cap_sold_out ON manacommunity.vendor_production_capacity(is_sold_out, production_date);

CREATE INDEX IF NOT EXISTS idx_batch_vendor_date ON manacommunity.food_production_batch(vendor_id, production_date);
CREATE INDEX IF NOT EXISTS idx_batch_status_expiry ON manacommunity.food_production_batch(batch_status, expiry_date_time);
CREATE INDEX IF NOT EXISTS idx_batch_prod_status ON manacommunity.food_production_batch(product_id, batch_status);

CREATE INDEX IF NOT EXISTS idx_breakfast_combo_active ON manacommunity.breakfast_combo(is_active);
CREATE INDEX IF NOT EXISTS idx_breakfast_combo_code ON manacommunity.breakfast_combo(combo_code);

CREATE INDEX IF NOT EXISTS idx_combo_item_combo ON manacommunity.breakfast_combo_item(combo_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_combo_item_product ON manacommunity.breakfast_combo_item(product_id);

CREATE INDEX IF NOT EXISTS idx_daily_basket_user ON manacommunity.daily_basket(user_id);
CREATE INDEX IF NOT EXISTS idx_daily_basket_comm_active ON manacommunity.daily_basket(community_id, is_active, is_paused);

CREATE INDEX IF NOT EXISTS idx_basket_item_basket ON manacommunity.daily_basket_item(basket_id, is_active);
CREATE INDEX IF NOT EXISTS idx_basket_item_prod ON manacommunity.daily_basket_item(product_id);

-- Seed Initial Fresh Batter & Breakfast Configurations
INSERT INTO manacommunity.fresh_food_product_config
(product_id, name, description, category, execution_mode, unit, pack_size, unit_price, shelf_life_hours, storage_temp_min, storage_temp_max, min_order_lead_hours, is_active)
VALUES
('BAT-IDLI-DOSA-1KG', 'Fresh Stone-Ground Idli & Dosa Batter', 'Naturally fermented stone-ground batter, preservative-free', 'BATTER', 'DAILY_BASKET', 'PACK', '1 Kg', 65.00, 48, 2.0, 4.0, 8, TRUE),
('BAT-RAGI-DOSA-1KG', 'Organic Ragi Dosa Batter', 'Nutrient-dense finger millet batter with low glycemic index', 'BATTER', 'DAILY_BASKET', 'PACK', '1 Kg', 75.00, 48, 2.0, 4.0, 8, TRUE),
('BAT-MULTI-MILLET-1KG', 'Multi-Millet Fermented Batter', 'Ancient grains blend (Foxtail, Kodo, Barnyard millet)', 'BATTER', 'DAILY_BASKET', 'PACK', '1 Kg', 85.00, 48, 2.0, 4.0, 8, TRUE),
('BAT-APPAM-1KG', 'Kerala Soft Appam Batter', 'Freshly ground with hints of coconut milk for fluffy vellayappam', 'BATTER', 'DAILY_BASKET', 'PACK', '1 Kg', 70.00, 36, 2.0, 4.0, 8, TRUE),
('MLK-A2-COW-1L', 'Farm-Fresh A2 Desi Cow Milk', 'Chilled farm milk delivered daily in chilled cold chain', 'DAIRY', 'DAILY_BASKET', 'PACK', '1 L', 90.00, 24, 2.0, 4.0, 8, TRUE),
('SID-CHUTNEY-COCO-250G', 'Homestyle Fresh Coconut Chutney', 'Tempered with mustard, curry leaves, and green chillies', 'CONDIMENT', 'DAILY_BASKET', 'CONTAINER', '250 g', 35.00, 24, 2.0, 4.0, 4, TRUE),
('SID-SAMBAR-MIX-500ML', 'Traditional Madras Tiffin Sambar', 'Freshly prepared lentil vegetable stew ready to warm', 'CONDIMENT', 'DAILY_BASKET', 'CONTAINER', '500 ml', 45.00, 24, 2.0, 4.0, 4, TRUE),
('BEV-FILTER-COFFEE-200ML', 'Authentic South Indian Decoction', 'Chicory blend 80:20 freshly extracted morning brew', 'BEVERAGE', 'DAILY_BASKET', 'BOTTLE', '200 ml', 50.00, 48, 2.0, 4.0, 4, TRUE)
ON CONFLICT (product_id) DO NOTHING;

-- Seed Sample Breakfast Combo
INSERT INTO manacommunity.breakfast_combo
(combo_code, name, description, category, base_price, discounted_price, is_active)
VALUES
('CMB-IDLI-FEAST', 'South Indian Sunrise Breakfast Combo', 'Complete morning tiffin setup with fresh batter, freshly ground coconut chutney, and filter coffee decoction', 'BREAKFAST', 150.00, 135.00, TRUE)
ON CONFLICT (combo_code) DO NOTHING;

-- Seed Combo BOM Items
INSERT INTO manacommunity.breakfast_combo_item
(combo_id, product_id, product_name, item_role, default_quantity, unit_price, is_mandatory, is_customizable, allowed_substitutes, sort_order)
SELECT c.id, 'BAT-IDLI-DOSA-1KG', 'Fresh Stone-Ground Idli & Dosa Batter', 'MAIN', 1, 65.00, TRUE, TRUE, 'BAT-RAGI-DOSA-1KG,BAT-MULTI-MILLET-1KG', 1
FROM manacommunity.breakfast_combo c WHERE c.combo_code = 'CMB-IDLI-FEAST'
AND NOT EXISTS (SELECT 1 FROM manacommunity.breakfast_combo_item ci WHERE ci.combo_id = c.id AND ci.product_id = 'BAT-IDLI-DOSA-1KG');

INSERT INTO manacommunity.breakfast_combo_item
(combo_id, product_id, product_name, item_role, default_quantity, unit_price, is_mandatory, is_customizable, allowed_substitutes, sort_order)
SELECT c.id, 'SID-CHUTNEY-COCO-250G', 'Homestyle Fresh Coconut Chutney', 'SIDE', 1, 35.00, TRUE, FALSE, NULL, 2
FROM manacommunity.breakfast_combo c WHERE c.combo_code = 'CMB-IDLI-FEAST'
AND NOT EXISTS (SELECT 1 FROM manacommunity.breakfast_combo_item ci WHERE ci.combo_id = c.id AND ci.product_id = 'SID-CHUTNEY-COCO-250G');

INSERT INTO manacommunity.breakfast_combo_item
(combo_id, product_id, product_name, item_role, default_quantity, unit_price, is_mandatory, is_customizable, allowed_substitutes, sort_order)
SELECT c.id, 'BEV-FILTER-COFFEE-200ML', 'Authentic South Indian Decoction', 'BEVERAGE', 1, 50.00, FALSE, TRUE, 'MLK-A2-COW-1L', 3
FROM manacommunity.breakfast_combo c WHERE c.combo_code = 'CMB-IDLI-FEAST'
AND NOT EXISTS (SELECT 1 FROM manacommunity.breakfast_combo_item ci WHERE ci.combo_id = c.id AND ci.product_id = 'BEV-FILTER-COFFEE-200ML');
