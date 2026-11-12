-- ============================================================
-- V261  Mana Fresh Wellness – Nutrition Subscription Module
-- ============================================================
-- New tables (5):
--   1. wellness_product_nutrition   – per-product nutritional data
--   2. wellness_community_cluster   – society-level delivery cluster
--   3. wellness_subscriber_preference – per-resident subscription prefs
--   4. wellness_menu_cycle          – 4-week rotating daily menu
--   5. wellness_menu_swap           – resident weekly swap requests
--
-- Re-used from Phase 1/2 (no changes):
--   commerce_order, daily_basket, daily_basket_item,
--   fresh_food_product_config, food_resident_allergy,
--   food_resident_profile, food_production_batch
-- ============================================================

SET search_path = manacommunity;

-- --------------------------------------------------------
-- 1. wellness_product_nutrition
--    Nutritional data for products in the wellness catalogue.
--    Linked 1:1 to fresh_food_product_config.
-- --------------------------------------------------------
CREATE TABLE wellness_product_nutrition (
    id                BIGSERIAL       PRIMARY KEY,
    product_id        BIGINT          NOT NULL UNIQUE
                                        REFERENCES fresh_food_product_config(id)
                                        ON DELETE CASCADE,
    energy_kcal       NUMERIC(8,2)    NOT NULL,
    protein_g         NUMERIC(8,2)    NOT NULL,
    carbs_g           NUMERIC(8,2)    NOT NULL,
    sugar_g           NUMERIC(8,2),
    fat_g             NUMERIC(8,2)    NOT NULL,
    saturated_fat_g   NUMERIC(8,2),
    fibre_g           NUMERIC(8,2),
    sodium_mg         NUMERIC(8,2),
    glycemic_index    INTEGER,
    per_serving_g     NUMERIC(8,2)    NOT NULL DEFAULT 100,
    -- Arrays stored as PostgreSQL TEXT arrays
    -- e.g. '{WEIGHT_LOSS,DIABETIC_FRIENDLY}'
    goal_tags         TEXT[]          DEFAULT '{}',
    allergen_flags    TEXT[]          DEFAULT '{}',
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wpn_product_id ON wellness_product_nutrition (product_id);
CREATE INDEX idx_wpn_goal_tags   ON wellness_product_nutrition USING gin (goal_tags);

-- --------------------------------------------------------
-- 2. wellness_community_cluster
--    Society-level delivery route tracking.
--    Cluster becomes ACTIVE when current_subscriber_count
--    reaches activation_threshold (default 15).
-- --------------------------------------------------------
CREATE TABLE wellness_community_cluster (
    id                          BIGSERIAL       PRIMARY KEY,
    society_name                VARCHAR(200)    NOT NULL,
    block                       VARCHAR(50),
    city                        VARCHAR(100)    NOT NULL DEFAULT 'Hyderabad',
    lat                         NUMERIC(10,7),
    lng                         NUMERIC(10,7),
    status                      VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    current_subscriber_count    INTEGER         NOT NULL DEFAULT 0,
    activation_threshold        INTEGER         NOT NULL DEFAULT 15,
    max_capacity                INTEGER         NOT NULL DEFAULT 80,
    delivery_start_time         TIME            NOT NULL DEFAULT '06:00:00',
    delivery_end_time           TIME            NOT NULL DEFAULT '08:00:00',
    version                     BIGINT          NOT NULL DEFAULT 0,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wcc_status ON wellness_community_cluster (status);

-- --------------------------------------------------------
-- 3. wellness_subscriber_preference
--    One row per active wellness subscriber (resident).
--    Links to wellness_community_cluster for route assignment.
-- --------------------------------------------------------
CREATE TABLE wellness_subscriber_preference (
    id                        BIGSERIAL       PRIMARY KEY,
    user_id                   BIGINT          NOT NULL UNIQUE,
    plan_type                 VARCHAR(30)     NOT NULL DEFAULT 'BASIC',
    portion_size              VARCHAR(20)     NOT NULL DEFAULT 'STANDARD',
    goal_type                 VARCHAR(40),
    disliked_ingredients      TEXT[]          DEFAULT '{}',
    extra_notes               TEXT,
    subscription_status       VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    pause_from                DATE,
    pause_until               DATE,
    cashfree_subscription_id  VARCHAR(150),
    cluster_id                BIGINT          REFERENCES wellness_community_cluster(id),
    created_at                TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wsp_user_id ON wellness_subscriber_preference (user_id);
CREATE INDEX idx_wsp_status  ON wellness_subscriber_preference (subscription_status);
CREATE INDEX idx_wsp_cluster ON wellness_subscriber_preference (cluster_id);

-- --------------------------------------------------------
-- 4. wellness_menu_cycle
--    4-week rotating daily menu per plan type.
--    (plan_type, cycle_week, day_of_week) is unique.
-- --------------------------------------------------------
CREATE TABLE wellness_menu_cycle (
    id               BIGSERIAL    PRIMARY KEY,
    plan_type        VARCHAR(30)  NOT NULL,
    cycle_week       SMALLINT     NOT NULL CHECK (cycle_week BETWEEN 1 AND 4),
    day_of_week      SMALLINT     NOT NULL CHECK (day_of_week BETWEEN 1 AND 6),
    product_ids      BIGINT[]     NOT NULL DEFAULT '{}',
    alt_product_ids  BIGINT[]     DEFAULT '{}',
    dietitian_notes  TEXT,
    approved_by      VARCHAR(100),
    approved_at      TIMESTAMPTZ,
    effective_from   DATE         NOT NULL,
    CONSTRAINT uq_wmc_plan_week_day UNIQUE (plan_type, cycle_week, day_of_week)
);

CREATE INDEX idx_wmc_plan_type ON wellness_menu_cycle (plan_type);
CREATE INDEX idx_wmc_effective ON wellness_menu_cycle (effective_from);

-- --------------------------------------------------------
-- 5. wellness_menu_swap
--    Resident-requested weekly product swaps.
--    Requests only accepted during Friday 18:00 – Saturday 20:00.
-- --------------------------------------------------------
CREATE TABLE wellness_menu_swap (
    id                    BIGSERIAL       PRIMARY KEY,
    subscriber_pref_id    BIGINT          NOT NULL
                                            REFERENCES wellness_subscriber_preference(id)
                                            ON DELETE CASCADE,
    delivery_date         DATE            NOT NULL,
    original_product_id   BIGINT          NOT NULL
                                            REFERENCES fresh_food_product_config(id),
    requested_product_id  BIGINT          NOT NULL
                                            REFERENCES fresh_food_product_config(id),
    status                VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    rejection_reason      TEXT,
    requested_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    resolved_at           TIMESTAMPTZ
);

CREATE INDEX idx_wms_sub_pref    ON wellness_menu_swap (subscriber_pref_id);
CREATE INDEX idx_wms_delivery_dt ON wellness_menu_swap (delivery_date);
CREATE INDEX idx_wms_status      ON wellness_menu_swap (status);

-- --------------------------------------------------------
-- Seed: initial menu cycles for BASIC plan (week 1, Mon–Sat)
-- Seeds reference fresh_food_product_config IDs seeded in V260.
-- product_ids are intentionally empty arrays here because
-- dietitian approval is required before activation.
-- --------------------------------------------------------
INSERT INTO wellness_menu_cycle
    (plan_type, cycle_week, day_of_week, product_ids, alt_product_ids, effective_from)
VALUES
    ('BASIC',   1, 1, '{}', '{}', CURRENT_DATE),
    ('BASIC',   1, 2, '{}', '{}', CURRENT_DATE),
    ('BASIC',   1, 3, '{}', '{}', CURRENT_DATE),
    ('BASIC',   1, 4, '{}', '{}', CURRENT_DATE),
    ('BASIC',   1, 5, '{}', '{}', CURRENT_DATE),
    ('BASIC',   1, 6, '{}', '{}', CURRENT_DATE),
    ('PREMIUM', 1, 1, '{}', '{}', CURRENT_DATE),
    ('PREMIUM', 1, 2, '{}', '{}', CURRENT_DATE),
    ('PREMIUM', 1, 3, '{}', '{}', CURRENT_DATE),
    ('PREMIUM', 1, 4, '{}', '{}', CURRENT_DATE),
    ('PREMIUM', 1, 5, '{}', '{}', CURRENT_DATE),
    ('PREMIUM', 1, 6, '{}', '{}', CURRENT_DATE);

-- Seed: Hyderabad pilot cluster
INSERT INTO wellness_community_cluster
    (society_name, block, city, status, activation_threshold, max_capacity)
VALUES
    ('Mana Premiere Residences', NULL, 'Hyderabad', 'PENDING', 15, 80),
    ('Aparna Sarovar Grande',    NULL, 'Hyderabad', 'PENDING', 15, 80),
    ('My Home Jewel',            NULL, 'Hyderabad', 'PENDING', 15, 80);
