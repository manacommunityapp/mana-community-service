-- FILE: db/sql/v1.0.0/37_community_food_os.sql
-- =====================================================================================
-- COMMUNITY FOOD & LIFESTYLE OPERATING SYSTEM (CFLOS) -- Complete Food Ecosystem
-- 150+ tables covering resident profiles, restaurants, home chefs, cloud kitchens,
-- community kitchens, subscriptions, ordering, delivery, grocery, inventory, recipes,
-- nutrition, events, corporate food, catering, AI, pantry, loyalty, reviews, payments,
-- notifications, workflows, and analytics.
-- =====================================================================================


-- =====================================================================================
-- 1. RESIDENT FOOD PROFILE ENGINE (8 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_resident_profiles (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    diet_type             VARCHAR(30)     NOT NULL DEFAULT 'NON_VEG',
    calorie_goal          INT,
    protein_goal          DECIMAL(8,2),
    weight_goal           DECIMAL(8,2),
    health_goal           VARCHAR(200),
    fitness_goal          VARCHAR(200),
    water_intake_goal     INT,
    coffee_limit          INT,
    daily_nutrition_score DECIMAL(5,2),
    ai_lifestyle_score    DECIMAL(5,2),
    bmi                   DECIMAL(5,2),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_food_resident_profiles_diet CHECK (diet_type IN ('VEGETARIAN','NON_VEG','VEGAN','JAIN','HALAL','KOSHER','EGGITARIAN'))
);

CREATE TABLE IF NOT EXISTS food_resident_allergies (
    id                    BIGSERIAL       PRIMARY KEY,
    profile_id            BIGINT          NOT NULL REFERENCES food_resident_profiles(id) ON DELETE CASCADE,
    allergy_name          VARCHAR(150)    NOT NULL,
    severity              VARCHAR(20)     NOT NULL DEFAULT 'MILD',
    notes                 VARCHAR(500),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_resident_allergies_severity CHECK (severity IN ('MILD','MODERATE','SEVERE'))
);

CREATE TABLE IF NOT EXISTS food_resident_medical_restrictions (
    id                    BIGSERIAL       PRIMARY KEY,
    profile_id            BIGINT          NOT NULL REFERENCES food_resident_profiles(id) ON DELETE CASCADE,
    restriction_name      VARCHAR(200)    NOT NULL,
    prescribed_by         VARCHAR(200),
    valid_until           DATE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_resident_cuisine_preferences (
    id                    BIGSERIAL       PRIMARY KEY,
    profile_id            BIGINT          NOT NULL REFERENCES food_resident_profiles(id) ON DELETE CASCADE,
    cuisine_name          VARCHAR(100)    NOT NULL,
    preference_level      INT             NOT NULL DEFAULT 3,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_resident_cuisine_pref_level CHECK (preference_level BETWEEN 1 AND 5)
);

CREATE TABLE IF NOT EXISTS food_resident_meal_timings (
    id                    BIGSERIAL       PRIMARY KEY,
    profile_id            BIGINT          NOT NULL REFERENCES food_resident_profiles(id) ON DELETE CASCADE,
    meal_type             VARCHAR(20)     NOT NULL,
    preferred_time        TIME            NOT NULL,
    flexibility_minutes   INT             DEFAULT 30,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_resident_meal_type CHECK (meal_type IN ('BREAKFAST','LUNCH','DINNER','SNACK'))
);

CREATE TABLE IF NOT EXISTS food_resident_favorites (
    id                    BIGSERIAL       PRIMARY KEY,
    profile_id            BIGINT          NOT NULL REFERENCES food_resident_profiles(id) ON DELETE CASCADE,
    favorite_type         VARCHAR(30)     NOT NULL,
    reference_id          BIGINT          NOT NULL,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_resident_fav_type CHECK (favorite_type IN ('RESTAURANT','HOME_CHEF','RECIPE','DISH'))
);

CREATE TABLE IF NOT EXISTS food_resident_family_members (
    id                    BIGSERIAL       PRIMARY KEY,
    profile_id            BIGINT          NOT NULL REFERENCES food_resident_profiles(id) ON DELETE CASCADE,
    name                  VARCHAR(150)    NOT NULL,
    relationship          VARCHAR(50),
    age                   INT,
    diet_type             VARCHAR(30),
    allergies             TEXT,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_resident_goals (
    id                    BIGSERIAL       PRIMARY KEY,
    profile_id            BIGINT          NOT NULL REFERENCES food_resident_profiles(id) ON DELETE CASCADE,
    goal_type             VARCHAR(50)     NOT NULL,
    target_value          DECIMAL(10,2),
    current_value         DECIMAL(10,2),
    unit                  VARCHAR(30),
    start_date            DATE,
    target_date           DATE,
    status                VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =====================================================================================
-- 2. RESTAURANT MANAGEMENT ENGINE (15 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_restaurants (
    id                    BIGSERIAL       PRIMARY KEY,
    name                  VARCHAR(200)    NOT NULL,
    slug                  VARCHAR(200),
    description           VARCHAR(2000),
    cuisine_types         TEXT[],
    address               VARCHAR(500),
    latitude              DECIMAL(10,7),
    longitude             DECIMAL(10,7),
    phone                 VARCHAR(20),
    email                 VARCHAR(150),
    logo_url              VARCHAR(500),
    cover_image_url       VARCHAR(500),
    fssai_license         VARCHAR(50),
    gst_number            VARCHAR(30),
    status                VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    rating                DECIMAL(3,2)    DEFAULT 0.00,
    total_ratings         INT             DEFAULT 0,
    commission_rate       DECIMAL(5,2)    DEFAULT 0.00,
    opening_time          TIME,
    closing_time          TIME,
    delivery_enabled      BOOLEAN         NOT NULL DEFAULT TRUE,
    takeaway_enabled      BOOLEAN         NOT NULL DEFAULT TRUE,
    dine_in_enabled       BOOLEAN         NOT NULL DEFAULT TRUE,
    min_order_amount      DECIMAL(10,2)   DEFAULT 0,
    avg_delivery_time     INT,
    featured              BOOLEAN         NOT NULL DEFAULT FALSE,
    verified              BOOLEAN         NOT NULL DEFAULT FALSE,
    owner_id              BIGINT          REFERENCES app_user(id),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_food_restaurants_status CHECK (status IN ('PENDING','APPROVED','SUSPENDED','CLOSED'))
);

CREATE TABLE IF NOT EXISTS food_restaurant_branches (
    id                    BIGSERIAL       PRIMARY KEY,
    restaurant_id         BIGINT          NOT NULL REFERENCES food_restaurants(id) ON DELETE CASCADE,
    branch_name           VARCHAR(200)    NOT NULL,
    address               VARCHAR(500),
    latitude              DECIMAL(10,7),
    longitude             DECIMAL(10,7),
    phone                 VARCHAR(20),
    manager_id            BIGINT          REFERENCES app_user(id),
    active                BOOLEAN         NOT NULL DEFAULT TRUE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_restaurant_operating_hours (
    id                    BIGSERIAL       PRIMARY KEY,
    restaurant_id         BIGINT          NOT NULL REFERENCES food_restaurants(id) ON DELETE CASCADE,
    day_of_week           INT             NOT NULL,
    open_time             TIME            NOT NULL,
    close_time            TIME            NOT NULL,
    is_closed             BOOLEAN         NOT NULL DEFAULT FALSE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_rest_hours_dow CHECK (day_of_week BETWEEN 0 AND 6)
);

CREATE TABLE IF NOT EXISTS food_menu_categories (
    id                    BIGSERIAL       PRIMARY KEY,
    restaurant_id         BIGINT          NOT NULL REFERENCES food_restaurants(id) ON DELETE CASCADE,
    name                  VARCHAR(150)    NOT NULL,
    slug                  VARCHAR(150),
    description           VARCHAR(500),
    image_url             VARCHAR(500),
    sort_order            INT             NOT NULL DEFAULT 0,
    active                BOOLEAN         NOT NULL DEFAULT TRUE,
    parent_id             BIGINT          REFERENCES food_menu_categories(id),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_menu_items (
    id                    BIGSERIAL       PRIMARY KEY,
    category_id           BIGINT          REFERENCES food_menu_categories(id) ON DELETE SET NULL,
    restaurant_id         BIGINT          NOT NULL REFERENCES food_restaurants(id) ON DELETE CASCADE,
    name                  VARCHAR(200)    NOT NULL,
    slug                  VARCHAR(200),
    description           VARCHAR(1000),
    image_url             VARCHAR(500),
    price                 DECIMAL(10,2)   NOT NULL,
    discounted_price      DECIMAL(10,2),
    is_veg                BOOLEAN         NOT NULL DEFAULT FALSE,
    is_vegan              BOOLEAN         NOT NULL DEFAULT FALSE,
    is_jain               BOOLEAN         NOT NULL DEFAULT FALSE,
    spice_level           INT             DEFAULT 0,
    calories              INT,
    protein               DECIMAL(8,2),
    carbs                 DECIMAL(8,2),
    fat                   DECIMAL(8,2),
    fiber                 DECIMAL(8,2),
    preparation_time      INT,
    is_available          BOOLEAN         NOT NULL DEFAULT TRUE,
    is_featured           BOOLEAN         NOT NULL DEFAULT FALSE,
    is_bestseller         BOOLEAN         NOT NULL DEFAULT FALSE,
    sort_order            INT             NOT NULL DEFAULT 0,
    tags                  TEXT[],
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_food_menu_items_price CHECK (price >= 0),
    CONSTRAINT chk_food_menu_items_spice CHECK (spice_level BETWEEN 0 AND 5)
);

CREATE TABLE IF NOT EXISTS food_menu_item_variants (
    id                    BIGSERIAL       PRIMARY KEY,
    item_id               BIGINT          NOT NULL REFERENCES food_menu_items(id) ON DELETE CASCADE,
    variant_name          VARCHAR(100)    NOT NULL,
    price                 DECIMAL(10,2)   NOT NULL,
    is_default            BOOLEAN         NOT NULL DEFAULT FALSE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_menu_variant_price CHECK (price >= 0)
);

CREATE TABLE IF NOT EXISTS food_menu_item_addons (
    id                    BIGSERIAL       PRIMARY KEY,
    item_id               BIGINT          NOT NULL REFERENCES food_menu_items(id) ON DELETE CASCADE,
    addon_group_name      VARCHAR(100),
    addon_name            VARCHAR(100)    NOT NULL,
    price                 DECIMAL(10,2)   NOT NULL DEFAULT 0,
    is_default            BOOLEAN         NOT NULL DEFAULT FALSE,
    max_quantity          INT             DEFAULT 1,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_menu_addon_price CHECK (price >= 0)
);

CREATE TABLE IF NOT EXISTS food_menu_item_combos (
    id                    BIGSERIAL       PRIMARY KEY,
    name                  VARCHAR(200)    NOT NULL,
    restaurant_id         BIGINT          NOT NULL REFERENCES food_restaurants(id) ON DELETE CASCADE,
    description           VARCHAR(1000),
    image_url             VARCHAR(500),
    combo_price           DECIMAL(10,2)   NOT NULL,
    original_price        DECIMAL(10,2),
    active                BOOLEAN         NOT NULL DEFAULT TRUE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_combo_price CHECK (combo_price >= 0)
);

CREATE TABLE IF NOT EXISTS food_menu_combo_items (
    id                    BIGSERIAL       PRIMARY KEY,
    combo_id              BIGINT          NOT NULL REFERENCES food_menu_item_combos(id) ON DELETE CASCADE,
    item_id               BIGINT          NOT NULL REFERENCES food_menu_items(id) ON DELETE CASCADE,
    quantity              INT             NOT NULL DEFAULT 1,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_restaurant_tables (
    id                    BIGSERIAL       PRIMARY KEY,
    restaurant_id         BIGINT          NOT NULL REFERENCES food_restaurants(id) ON DELETE CASCADE,
    table_number          VARCHAR(20)     NOT NULL,
    capacity              INT             NOT NULL DEFAULT 2,
    location              VARCHAR(30)     NOT NULL DEFAULT 'INDOOR',
    status                VARCHAR(30)     NOT NULL DEFAULT 'AVAILABLE',
    qr_code               VARCHAR(500),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_rest_table_location CHECK (location IN ('INDOOR','OUTDOOR','TERRACE','PRIVATE')),
    CONSTRAINT chk_food_rest_table_status CHECK (status IN ('AVAILABLE','OCCUPIED','RESERVED','MAINTENANCE'))
);

CREATE TABLE IF NOT EXISTS food_restaurant_staff (
    id                    BIGSERIAL       PRIMARY KEY,
    restaurant_id         BIGINT          NOT NULL REFERENCES food_restaurants(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    role                  VARCHAR(30)     NOT NULL,
    status                VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    joined_at             TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_rest_staff_role CHECK (role IN ('OWNER','MANAGER','CHEF','WAITER','DELIVERY'))
);

CREATE TABLE IF NOT EXISTS food_restaurant_reviews (
    id                    BIGSERIAL       PRIMARY KEY,
    restaurant_id         BIGINT          NOT NULL REFERENCES food_restaurants(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    rating                INT             NOT NULL,
    title                 VARCHAR(200),
    review_text           VARCHAR(2000),
    food_rating           INT,
    service_rating        INT,
    ambiance_rating       INT,
    value_rating          INT,
    images                TEXT[],
    helpful_count         INT             DEFAULT 0,
    reported              BOOLEAN         NOT NULL DEFAULT FALSE,
    reply_text            VARCHAR(1000),
    reply_at              TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_food_rest_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_food_rest_review_food CHECK (food_rating IS NULL OR food_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_food_rest_review_service CHECK (service_rating IS NULL OR service_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_food_rest_review_ambiance CHECK (ambiance_rating IS NULL OR ambiance_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_food_rest_review_value CHECK (value_rating IS NULL OR value_rating BETWEEN 1 AND 5)
);

CREATE TABLE IF NOT EXISTS food_restaurant_offers (
    id                    BIGSERIAL       PRIMARY KEY,
    restaurant_id         BIGINT          NOT NULL REFERENCES food_restaurants(id) ON DELETE CASCADE,
    title                 VARCHAR(200)    NOT NULL,
    description           VARCHAR(1000),
    offer_type            VARCHAR(30)     NOT NULL,
    discount_value        DECIMAL(10,2)   NOT NULL,
    min_order             DECIMAL(10,2)   DEFAULT 0,
    max_discount          DECIMAL(10,2),
    valid_from            TIMESTAMP       NOT NULL,
    valid_until           TIMESTAMP       NOT NULL,
    usage_limit           INT,
    used_count            INT             DEFAULT 0,
    coupon_code           VARCHAR(50),
    terms                 TEXT,
    active                BOOLEAN         NOT NULL DEFAULT TRUE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_rest_offer_type CHECK (offer_type IN ('PERCENTAGE','FLAT','BOGO','FREEBIE')),
    CONSTRAINT chk_food_rest_offer_value CHECK (discount_value >= 0)
);

CREATE TABLE IF NOT EXISTS food_restaurant_analytics (
    id                    BIGSERIAL       PRIMARY KEY,
    restaurant_id         BIGINT          NOT NULL REFERENCES food_restaurants(id) ON DELETE CASCADE,
    date                  DATE            NOT NULL,
    total_orders          INT             DEFAULT 0,
    total_revenue         DECIMAL(14,2)   DEFAULT 0,
    avg_order_value       DECIMAL(10,2)   DEFAULT 0,
    new_customers         INT             DEFAULT 0,
    repeat_customers      INT             DEFAULT 0,
    avg_preparation_time  INT,
    avg_delivery_time     INT,
    cancellation_rate     DECIMAL(5,2)    DEFAULT 0,
    rating_avg            DECIMAL(3,2),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_rest_analytics_cancel CHECK (cancellation_rate BETWEEN 0 AND 100)
);

CREATE TABLE IF NOT EXISTS food_restaurant_documents (
    id                    BIGSERIAL       PRIMARY KEY,
    restaurant_id         BIGINT          NOT NULL REFERENCES food_restaurants(id) ON DELETE CASCADE,
    document_type         VARCHAR(50)     NOT NULL,
    document_url          VARCHAR(500)    NOT NULL,
    verified              BOOLEAN         NOT NULL DEFAULT FALSE,
    verified_by           BIGINT          REFERENCES app_user(id),
    verified_at           TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =====================================================================================
-- 3. HOME CHEF ENGINE (8 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_home_chefs (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    kitchen_name          VARCHAR(200)    NOT NULL,
    description           VARCHAR(2000),
    speciality            VARCHAR(200),
    cuisine_types         TEXT[],
    fssai_license         VARCHAR(50),
    status                VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    verification_status   VARCHAR(30)     DEFAULT 'PENDING',
    max_orders_per_day    INT             DEFAULT 10,
    rating                DECIMAL(3,2)    DEFAULT 0.00,
    total_ratings         INT             DEFAULT 0,
    total_orders          INT             DEFAULT 0,
    revenue_total         DECIMAL(14,2)   DEFAULT 0,
    commission_rate       DECIMAL(5,2)    DEFAULT 0.00,
    profile_image_url     VARCHAR(500),
    cover_image_url       VARCHAR(500),
    availability_status   VARCHAR(30)     DEFAULT 'AVAILABLE',
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_food_home_chefs_status CHECK (status IN ('PENDING','APPROVED','SUSPENDED'))
);

CREATE TABLE IF NOT EXISTS food_home_chef_menu (
    id                    BIGSERIAL       PRIMARY KEY,
    chef_id               BIGINT          NOT NULL REFERENCES food_home_chefs(id) ON DELETE CASCADE,
    name                  VARCHAR(200)    NOT NULL,
    description           VARCHAR(1000),
    image_url             VARCHAR(500),
    price                 DECIMAL(10,2)   NOT NULL,
    category              VARCHAR(100),
    is_veg                BOOLEAN         NOT NULL DEFAULT FALSE,
    calories              INT,
    protein               DECIMAL(8,2),
    preparation_time      INT,
    available_days        TEXT[],
    order_before_time     TIME,
    max_quantity          INT,
    sort_order            INT             NOT NULL DEFAULT 0,
    active                BOOLEAN         NOT NULL DEFAULT TRUE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_home_chef_menu_price CHECK (price >= 0)
);

CREATE TABLE IF NOT EXISTS food_home_chef_certifications (
    id                    BIGSERIAL       PRIMARY KEY,
    chef_id               BIGINT          NOT NULL REFERENCES food_home_chefs(id) ON DELETE CASCADE,
    certification_name    VARCHAR(200)    NOT NULL,
    issuing_authority     VARCHAR(200),
    certificate_url       VARCHAR(500),
    valid_until           DATE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_home_chef_operating_hours (
    id                    BIGSERIAL       PRIMARY KEY,
    chef_id               BIGINT          NOT NULL REFERENCES food_home_chefs(id) ON DELETE CASCADE,
    day_of_week           INT             NOT NULL,
    available             BOOLEAN         NOT NULL DEFAULT TRUE,
    start_time            TIME,
    end_time              TIME,
    max_orders            INT,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_home_chef_hours_dow CHECK (day_of_week BETWEEN 0 AND 6)
);

CREATE TABLE IF NOT EXISTS food_home_chef_reviews (
    id                    BIGSERIAL       PRIMARY KEY,
    chef_id               BIGINT          NOT NULL REFERENCES food_home_chefs(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    order_id              BIGINT,
    rating                INT             NOT NULL,
    review_text           VARCHAR(2000),
    taste_rating          INT,
    hygiene_rating        INT,
    packaging_rating      INT,
    value_rating          INT,
    images                TEXT[],
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_home_chef_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_food_home_chef_review_taste CHECK (taste_rating IS NULL OR taste_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_food_home_chef_review_hygiene CHECK (hygiene_rating IS NULL OR hygiene_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_food_home_chef_review_packaging CHECK (packaging_rating IS NULL OR packaging_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_food_home_chef_review_value CHECK (value_rating IS NULL OR value_rating BETWEEN 1 AND 5)
);

CREATE TABLE IF NOT EXISTS food_home_chef_payouts (
    id                    BIGSERIAL       PRIMARY KEY,
    chef_id               BIGINT          NOT NULL REFERENCES food_home_chefs(id) ON DELETE CASCADE,
    amount                DECIMAL(12,2)   NOT NULL,
    period_start          DATE            NOT NULL,
    period_end            DATE            NOT NULL,
    status                VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    transaction_ref       VARCHAR(100),
    paid_at               TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_home_chef_payout_status CHECK (status IN ('PENDING','PROCESSING','COMPLETED','FAILED')),
    CONSTRAINT chk_food_home_chef_payout_amt CHECK (amount >= 0)
);

CREATE TABLE IF NOT EXISTS food_home_chef_specialties (
    id                    BIGSERIAL       PRIMARY KEY,
    chef_id               BIGINT          NOT NULL REFERENCES food_home_chefs(id) ON DELETE CASCADE,
    specialty_name        VARCHAR(150)    NOT NULL,
    description           VARCHAR(500),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_home_chef_gallery (
    id                    BIGSERIAL       PRIMARY KEY,
    chef_id               BIGINT          NOT NULL REFERENCES food_home_chefs(id) ON DELETE CASCADE,
    image_url             VARCHAR(500)    NOT NULL,
    caption               VARCHAR(300),
    sort_order            INT             NOT NULL DEFAULT 0,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =====================================================================================
-- 4. CLOUD KITCHEN ENGINE (6 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_cloud_kitchens (
    id                    BIGSERIAL       PRIMARY KEY,
    name                  VARCHAR(200)    NOT NULL,
    description           VARCHAR(2000),
    address               VARCHAR(500),
    latitude              DECIMAL(10,7),
    longitude             DECIMAL(10,7),
    owner_id              BIGINT          REFERENCES app_user(id),
    capacity              INT,
    status                VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    license_number        VARCHAR(100),
    kitchen_type          VARCHAR(30)     NOT NULL DEFAULT 'SHARED',
    rent                  DECIMAL(12,2),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_food_cloud_kitchen_type CHECK (kitchen_type IN ('SHARED','DEDICATED','VIRTUAL'))
);

CREATE TABLE IF NOT EXISTS food_cloud_kitchen_brands (
    id                    BIGSERIAL       PRIMARY KEY,
    kitchen_id            BIGINT          NOT NULL REFERENCES food_cloud_kitchens(id) ON DELETE CASCADE,
    brand_name            VARCHAR(200)    NOT NULL,
    slug                  VARCHAR(200),
    description           VARCHAR(1000),
    logo_url              VARCHAR(500),
    cuisine_type          VARCHAR(100),
    status                VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    rating                DECIMAL(3,2)    DEFAULT 0.00,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_cloud_kitchen_slots (
    id                    BIGSERIAL       PRIMARY KEY,
    kitchen_id            BIGINT          NOT NULL REFERENCES food_cloud_kitchens(id) ON DELETE CASCADE,
    brand_id              BIGINT          REFERENCES food_cloud_kitchen_brands(id) ON DELETE SET NULL,
    day_of_week           INT             NOT NULL,
    start_time            TIME            NOT NULL,
    end_time              TIME            NOT NULL,
    status                VARCHAR(30)     NOT NULL DEFAULT 'AVAILABLE',
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_cloud_slot_dow CHECK (day_of_week BETWEEN 0 AND 6)
);

CREATE TABLE IF NOT EXISTS food_cloud_kitchen_equipment (
    id                    BIGSERIAL       PRIMARY KEY,
    kitchen_id            BIGINT          NOT NULL REFERENCES food_cloud_kitchens(id) ON DELETE CASCADE,
    equipment_name        VARCHAR(200)    NOT NULL,
    quantity              INT             NOT NULL DEFAULT 1,
    status                VARCHAR(30)     NOT NULL DEFAULT 'OPERATIONAL',
    last_maintenance      DATE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_cloud_kitchen_production (
    id                    BIGSERIAL       PRIMARY KEY,
    kitchen_id            BIGINT          NOT NULL REFERENCES food_cloud_kitchens(id) ON DELETE CASCADE,
    brand_id              BIGINT          REFERENCES food_cloud_kitchen_brands(id) ON DELETE SET NULL,
    date                  DATE            NOT NULL,
    item_id               BIGINT,
    planned_quantity      INT             DEFAULT 0,
    actual_quantity       INT             DEFAULT 0,
    wastage               INT             DEFAULT 0,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_cloud_kitchen_analytics (
    id                    BIGSERIAL       PRIMARY KEY,
    kitchen_id            BIGINT          NOT NULL REFERENCES food_cloud_kitchens(id) ON DELETE CASCADE,
    date                  DATE            NOT NULL,
    total_orders          INT             DEFAULT 0,
    revenue               DECIMAL(14,2)   DEFAULT 0,
    utilization_pct       DECIMAL(5,2)    DEFAULT 0,
    wastage_pct           DECIMAL(5,2)    DEFAULT 0,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_cloud_analytics_util CHECK (utilization_pct BETWEEN 0 AND 100),
    CONSTRAINT chk_food_cloud_analytics_waste CHECK (wastage_pct BETWEEN 0 AND 100)
);


-- =====================================================================================
-- 5. COMMUNITY KITCHEN ENGINE (7 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_community_kitchens (
    id                    BIGSERIAL       PRIMARY KEY,
    name                  VARCHAR(200)    NOT NULL,
    kitchen_type          VARCHAR(30)     NOT NULL DEFAULT 'CLUBHOUSE',
    location              VARCHAR(500),
    description           VARCHAR(2000),
    capacity              INT,
    manager_id            BIGINT          REFERENCES app_user(id),
    status                VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    opening_time          TIME,
    closing_time          TIME,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_food_comm_kitchen_type CHECK (kitchen_type IN ('CLUBHOUSE','CAFETERIA','TEMPLE','CORPORATE','SENIOR_CITIZEN'))
);

CREATE TABLE IF NOT EXISTS food_community_kitchen_menus (
    id                    BIGSERIAL       PRIMARY KEY,
    kitchen_id            BIGINT          NOT NULL REFERENCES food_community_kitchens(id) ON DELETE CASCADE,
    date                  DATE            NOT NULL,
    meal_type             VARCHAR(20)     NOT NULL,
    items                 JSONB,
    price_per_plate       DECIMAL(10,2)   NOT NULL,
    total_plates          INT             NOT NULL,
    booked_plates         INT             DEFAULT 0,
    cutoff_time           TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_comm_menu_meal CHECK (meal_type IN ('BREAKFAST','LUNCH','DINNER','SNACK')),
    CONSTRAINT chk_food_comm_menu_price CHECK (price_per_plate >= 0)
);

CREATE TABLE IF NOT EXISTS food_community_kitchen_bookings (
    id                    BIGSERIAL       PRIMARY KEY,
    menu_id               BIGINT          NOT NULL REFERENCES food_community_kitchen_menus(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    quantity              INT             NOT NULL DEFAULT 1,
    total_amount          DECIMAL(10,2)   NOT NULL,
    status                VARCHAR(30)     NOT NULL DEFAULT 'BOOKED',
    pickup_code           VARCHAR(20),
    pickup_time           TIMESTAMP,
    picked_up_at          TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_comm_booking_status CHECK (status IN ('BOOKED','CONFIRMED','PICKED_UP','CANCELLED')),
    CONSTRAINT chk_food_comm_booking_amt CHECK (total_amount >= 0)
);

CREATE TABLE IF NOT EXISTS food_community_kitchen_tokens (
    id                    BIGSERIAL       PRIMARY KEY,
    booking_id            BIGINT          NOT NULL REFERENCES food_community_kitchen_bookings(id) ON DELETE CASCADE,
    token_number          VARCHAR(30)     NOT NULL,
    qr_code               VARCHAR(500),
    status                VARCHAR(20)     NOT NULL DEFAULT 'VALID',
    used_at               TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_comm_token_status CHECK (status IN ('VALID','USED','EXPIRED'))
);

CREATE TABLE IF NOT EXISTS food_community_kitchen_staff (
    id                    BIGSERIAL       PRIMARY KEY,
    kitchen_id            BIGINT          NOT NULL REFERENCES food_community_kitchens(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    role                  VARCHAR(50)     NOT NULL,
    shift                 VARCHAR(30),
    active                BOOLEAN         NOT NULL DEFAULT TRUE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_community_kitchen_waste (
    id                    BIGSERIAL       PRIMARY KEY,
    kitchen_id            BIGINT          NOT NULL REFERENCES food_community_kitchens(id) ON DELETE CASCADE,
    date                  DATE            NOT NULL,
    meal_type             VARCHAR(20)     NOT NULL,
    food_prepared_kg      DECIMAL(10,2)   NOT NULL,
    food_consumed_kg      DECIMAL(10,2)   NOT NULL,
    waste_kg              DECIMAL(10,2)   NOT NULL,
    waste_type            VARCHAR(50),
    disposal_method       VARCHAR(100),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_community_kitchen_feedback (
    id                    BIGSERIAL       PRIMARY KEY,
    kitchen_id            BIGINT          NOT NULL REFERENCES food_community_kitchens(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    date                  DATE            NOT NULL,
    meal_type             VARCHAR(20)     NOT NULL,
    rating                INT             NOT NULL,
    comments              VARCHAR(1000),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_comm_feedback_rating CHECK (rating BETWEEN 1 AND 5)
);


-- =====================================================================================
-- 6. MEAL SUBSCRIPTION ENGINE (6 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_subscription_plans (
    id                    BIGSERIAL       PRIMARY KEY,
    name                  VARCHAR(200)    NOT NULL,
    description           VARCHAR(1000),
    plan_type             VARCHAR(30)     NOT NULL,
    target_audience       VARCHAR(30)     NOT NULL DEFAULT 'GENERAL',
    provider_type         VARCHAR(30)     NOT NULL,
    provider_id           BIGINT,
    price_per_meal        DECIMAL(10,2)   NOT NULL,
    monthly_price         DECIMAL(12,2),
    min_days              INT             DEFAULT 1,
    includes_weekends     BOOLEAN         NOT NULL DEFAULT TRUE,
    active                BOOLEAN         NOT NULL DEFAULT TRUE,
    image_url             VARCHAR(500),
    nutrition_info        JSONB,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_sub_plan_type CHECK (plan_type IN ('BREAKFAST','LUNCH','DINNER','FULL_DAY')),
    CONSTRAINT chk_food_sub_plan_audience CHECK (target_audience IN ('GENERAL','KIDS','GYM','DIABETIC','PREGNANCY','SENIOR','OFFICE','DIET')),
    CONSTRAINT chk_food_sub_plan_provider CHECK (provider_type IN ('RESTAURANT','HOME_CHEF','CLOUD_KITCHEN','COMMUNITY_KITCHEN')),
    CONSTRAINT chk_food_sub_plan_price CHECK (price_per_meal >= 0)
);

CREATE TABLE IF NOT EXISTS food_subscriptions (
    id                    BIGSERIAL       PRIMARY KEY,
    plan_id               BIGINT          NOT NULL REFERENCES food_subscription_plans(id),
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    start_date            DATE            NOT NULL,
    end_date              DATE            NOT NULL,
    status                VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    auto_renew            BOOLEAN         NOT NULL DEFAULT FALSE,
    delivery_address      TEXT,
    delivery_instructions VARCHAR(500),
    payment_method        VARCHAR(50),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_sub_status CHECK (status IN ('ACTIVE','PAUSED','CANCELLED','EXPIRED'))
);

CREATE TABLE IF NOT EXISTS food_subscription_deliveries (
    id                    BIGSERIAL       PRIMARY KEY,
    subscription_id       BIGINT          NOT NULL REFERENCES food_subscriptions(id) ON DELETE CASCADE,
    date                  DATE            NOT NULL,
    meal_type             VARCHAR(20)     NOT NULL,
    status                VARCHAR(30)     NOT NULL DEFAULT 'SCHEDULED',
    delivered_at          TIMESTAMP,
    delivery_partner_id   BIGINT          REFERENCES app_user(id),
    feedback_rating       INT,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_sub_del_status CHECK (status IN ('SCHEDULED','PREPARING','OUT_FOR_DELIVERY','DELIVERED','SKIPPED','CANCELLED')),
    CONSTRAINT chk_food_sub_del_rating CHECK (feedback_rating IS NULL OR feedback_rating BETWEEN 1 AND 5)
);

CREATE TABLE IF NOT EXISTS food_subscription_pauses (
    id                    BIGSERIAL       PRIMARY KEY,
    subscription_id       BIGINT          NOT NULL REFERENCES food_subscriptions(id) ON DELETE CASCADE,
    pause_start           DATE            NOT NULL,
    pause_end             DATE            NOT NULL,
    reason                VARCHAR(500),
    status                VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_subscription_customizations (
    id                    BIGSERIAL       PRIMARY KEY,
    subscription_id       BIGINT          NOT NULL REFERENCES food_subscriptions(id) ON DELETE CASCADE,
    day_of_week           INT,
    meal_preference       VARCHAR(100),
    exclude_ingredients   TEXT[],
    notes                 VARCHAR(500),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_subscription_invoices (
    id                    BIGSERIAL       PRIMARY KEY,
    subscription_id       BIGINT          NOT NULL REFERENCES food_subscriptions(id) ON DELETE CASCADE,
    invoice_number        VARCHAR(50)     NOT NULL UNIQUE,
    period_start          DATE            NOT NULL,
    period_end            DATE            NOT NULL,
    total_meals           INT             NOT NULL DEFAULT 0,
    amount                DECIMAL(12,2)   NOT NULL,
    tax                   DECIMAL(10,2)   DEFAULT 0,
    discount              DECIMAL(10,2)   DEFAULT 0,
    final_amount          DECIMAL(12,2)   NOT NULL,
    status                VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    paid_at               TIMESTAMP,
    payment_ref           VARCHAR(100),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_sub_invoice_status CHECK (status IN ('PENDING','PAID','OVERDUE')),
    CONSTRAINT chk_food_sub_invoice_amt CHECK (final_amount >= 0)
);


-- =====================================================================================
-- 7. FOOD ORDERING ENGINE (8 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_orders (
    id                    BIGSERIAL       PRIMARY KEY,
    order_number          VARCHAR(50)     NOT NULL UNIQUE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    provider_type         VARCHAR(30)     NOT NULL,
    provider_id           BIGINT          NOT NULL,
    order_type            VARCHAR(20)     NOT NULL DEFAULT 'DELIVERY',
    status                VARCHAR(30)     NOT NULL DEFAULT 'PLACED',
    subtotal              DECIMAL(12,2)   NOT NULL,
    tax                   DECIMAL(10,2)   DEFAULT 0,
    delivery_fee          DECIMAL(10,2)   DEFAULT 0,
    discount              DECIMAL(10,2)   DEFAULT 0,
    total_amount          DECIMAL(12,2)   NOT NULL,
    payment_status        VARCHAR(30)     DEFAULT 'PENDING',
    payment_method        VARCHAR(50),
    delivery_address      TEXT,
    delivery_latitude     DECIMAL(10,7),
    delivery_longitude    DECIMAL(10,7),
    delivery_instructions VARCHAR(500),
    estimated_delivery    TIMESTAMP,
    actual_delivery       TIMESTAMP,
    placed_at             TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at          TIMESTAMP,
    preparing_at          TIMESTAMP,
    ready_at              TIMESTAMP,
    delivered_at          TIMESTAMP,
    cancelled_at          TIMESTAMP,
    cancellation_reason   VARCHAR(500),
    is_group_order        BOOLEAN         NOT NULL DEFAULT FALSE,
    group_order_id        BIGINT,
    scheduled_for         TIMESTAMP,
    is_gift               BOOLEAN         NOT NULL DEFAULT FALSE,
    gift_message          VARCHAR(500),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_orders_provider CHECK (provider_type IN ('RESTAURANT','HOME_CHEF','CLOUD_KITCHEN')),
    CONSTRAINT chk_food_orders_type CHECK (order_type IN ('DELIVERY','TAKEAWAY','DINE_IN')),
    CONSTRAINT chk_food_orders_status CHECK (status IN ('PLACED','CONFIRMED','PREPARING','READY','OUT_FOR_DELIVERY','DELIVERED','CANCELLED','REFUNDED')),
    CONSTRAINT chk_food_orders_total CHECK (total_amount >= 0)
);

CREATE TABLE IF NOT EXISTS food_order_items (
    id                    BIGSERIAL       PRIMARY KEY,
    order_id              BIGINT          NOT NULL REFERENCES food_orders(id) ON DELETE CASCADE,
    item_id               BIGINT,
    item_name             VARCHAR(200)    NOT NULL,
    quantity              INT             NOT NULL DEFAULT 1,
    unit_price            DECIMAL(10,2)   NOT NULL,
    total_price           DECIMAL(10,2)   NOT NULL,
    variant_name          VARCHAR(100),
    special_instructions  VARCHAR(500),
    is_veg                BOOLEAN,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_order_items_qty CHECK (quantity > 0),
    CONSTRAINT chk_food_order_items_price CHECK (unit_price >= 0)
);

CREATE TABLE IF NOT EXISTS food_order_item_addons (
    id                    BIGSERIAL       PRIMARY KEY,
    order_item_id         BIGINT          NOT NULL REFERENCES food_order_items(id) ON DELETE CASCADE,
    addon_name            VARCHAR(100)    NOT NULL,
    price                 DECIMAL(10,2)   NOT NULL DEFAULT 0,
    quantity              INT             NOT NULL DEFAULT 1,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_order_addon_price CHECK (price >= 0)
);

CREATE TABLE IF NOT EXISTS food_order_tracking (
    id                    BIGSERIAL       PRIMARY KEY,
    order_id              BIGINT          NOT NULL REFERENCES food_orders(id) ON DELETE CASCADE,
    status                VARCHAR(30)     NOT NULL,
    latitude              DECIMAL(10,7),
    longitude             DECIMAL(10,7),
    notes                 VARCHAR(500),
    timestamp             TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_order_ratings (
    id                    BIGSERIAL       PRIMARY KEY,
    order_id              BIGINT          NOT NULL REFERENCES food_orders(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    overall_rating        INT             NOT NULL,
    food_rating           INT,
    delivery_rating       INT,
    packaging_rating      INT,
    review_text           VARCHAR(2000),
    images                TEXT[],
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_order_rating CHECK (overall_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_food_order_rating_food CHECK (food_rating IS NULL OR food_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_food_order_rating_delivery CHECK (delivery_rating IS NULL OR delivery_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_food_order_rating_packaging CHECK (packaging_rating IS NULL OR packaging_rating BETWEEN 1 AND 5)
);

CREATE TABLE IF NOT EXISTS food_group_orders (
    id                    BIGSERIAL       PRIMARY KEY,
    created_by            BIGINT          NOT NULL REFERENCES app_user(id),
    title                 VARCHAR(200),
    status                VARCHAR(30)     NOT NULL DEFAULT 'OPEN',
    provider_type         VARCHAR(30),
    provider_id           BIGINT,
    join_code             VARCHAR(20),
    expires_at            TIMESTAMP,
    max_participants      INT,
    split_type            VARCHAR(20)     NOT NULL DEFAULT 'INDIVIDUAL',
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_group_order_split CHECK (split_type IN ('EQUAL','INDIVIDUAL'))
);

CREATE TABLE IF NOT EXISTS food_group_order_participants (
    id                    BIGSERIAL       PRIMARY KEY,
    group_order_id        BIGINT          NOT NULL REFERENCES food_group_orders(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    status                VARCHAR(30)     NOT NULL DEFAULT 'JOINED',
    individual_total      DECIMAL(10,2)   DEFAULT 0,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_order_refunds (
    id                    BIGSERIAL       PRIMARY KEY,
    order_id              BIGINT          NOT NULL REFERENCES food_orders(id) ON DELETE CASCADE,
    amount                DECIMAL(12,2)   NOT NULL,
    reason                VARCHAR(500),
    status                VARCHAR(30)     NOT NULL DEFAULT 'REQUESTED',
    processed_by          BIGINT          REFERENCES app_user(id),
    processed_at          TIMESTAMP,
    refund_method         VARCHAR(50),
    transaction_ref       VARCHAR(100),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_refund_status CHECK (status IN ('REQUESTED','APPROVED','PROCESSED','REJECTED')),
    CONSTRAINT chk_food_refund_amt CHECK (amount >= 0)
);


-- =====================================================================================
-- 8. DINING RESERVATION ENGINE (5 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_dining_reservations (
    id                    BIGSERIAL       PRIMARY KEY,
    restaurant_id         BIGINT          NOT NULL REFERENCES food_restaurants(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    reservation_type      VARCHAR(30)     NOT NULL DEFAULT 'RESTAURANT',
    date                  DATE            NOT NULL,
    time                  TIME            NOT NULL,
    party_size            INT             NOT NULL DEFAULT 2,
    status                VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    table_id              BIGINT          REFERENCES food_restaurant_tables(id),
    special_requests      TEXT,
    occasion              VARCHAR(100),
    pre_order_id          BIGINT,
    confirmation_code     VARCHAR(20),
    checked_in_at         TIMESTAMP,
    checked_out_at        TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_dining_res_type CHECK (reservation_type IN ('RESTAURANT','CLUBHOUSE','PARTY_HALL','POOLSIDE','CHEF_TABLE')),
    CONSTRAINT chk_food_dining_res_status CHECK (status IN ('PENDING','CONFIRMED','SEATED','COMPLETED','CANCELLED','NO_SHOW'))
);

CREATE TABLE IF NOT EXISTS food_dining_waitlist (
    id                    BIGSERIAL       PRIMARY KEY,
    restaurant_id         BIGINT          NOT NULL REFERENCES food_restaurants(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    party_size            INT             NOT NULL DEFAULT 2,
    estimated_wait        INT,
    status                VARCHAR(20)     NOT NULL DEFAULT 'WAITING',
    joined_at             TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notified_at           TIMESTAMP,
    seated_at             TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_dining_waitlist_status CHECK (status IN ('WAITING','NOTIFIED','SEATED','LEFT'))
);

CREATE TABLE IF NOT EXISTS food_dining_events (
    id                    BIGSERIAL       PRIMARY KEY,
    name                  VARCHAR(200)    NOT NULL,
    description           VARCHAR(2000),
    venue                 VARCHAR(300),
    event_type            VARCHAR(30)     NOT NULL,
    date                  DATE            NOT NULL,
    time                  TIME            NOT NULL,
    capacity              INT             NOT NULL,
    booked                INT             DEFAULT 0,
    price                 DECIMAL(10,2)   DEFAULT 0,
    menu                  JSONB,
    image_url             VARCHAR(500),
    organizer_id          BIGINT          REFERENCES app_user(id),
    status                VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_dining_event_type CHECK (event_type IN ('WEEKEND_BUFFET','FESTIVAL_DINNER','CHEF_TABLE','WINE_TASTING','BBQ')),
    CONSTRAINT chk_food_dining_event_price CHECK (price >= 0)
);

CREATE TABLE IF NOT EXISTS food_dining_pre_orders (
    id                    BIGSERIAL       PRIMARY KEY,
    reservation_id        BIGINT          NOT NULL REFERENCES food_dining_reservations(id) ON DELETE CASCADE,
    items                 JSONB,
    total_amount          DECIMAL(12,2)   DEFAULT 0,
    status                VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_dining_feedback (
    id                    BIGSERIAL       PRIMARY KEY,
    reservation_id        BIGINT          NOT NULL REFERENCES food_dining_reservations(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    overall_rating        INT             NOT NULL,
    food_rating           INT,
    service_rating        INT,
    ambiance_rating       INT,
    comments              VARCHAR(2000),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_dining_fb_rating CHECK (overall_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_food_dining_fb_food CHECK (food_rating IS NULL OR food_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_food_dining_fb_service CHECK (service_rating IS NULL OR service_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_food_dining_fb_ambiance CHECK (ambiance_rating IS NULL OR ambiance_rating BETWEEN 1 AND 5)
);


-- =====================================================================================
-- 9. FOOD DELIVERY ENGINE (6 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_delivery_partners (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    vehicle_type          VARCHAR(20)     NOT NULL,
    vehicle_number        VARCHAR(30),
    license_number        VARCHAR(50),
    status                VARCHAR(30)     NOT NULL DEFAULT 'OFFLINE',
    current_latitude      DECIMAL(10,7),
    current_longitude     DECIMAL(10,7),
    rating                DECIMAL(3,2)    DEFAULT 0.00,
    total_deliveries      INT             DEFAULT 0,
    active_zone           VARCHAR(100),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_food_del_partner_vehicle CHECK (vehicle_type IN ('BICYCLE','MOTORCYCLE','CAR')),
    CONSTRAINT chk_food_del_partner_status CHECK (status IN ('AVAILABLE','ON_DELIVERY','OFFLINE','SUSPENDED'))
);

CREATE TABLE IF NOT EXISTS food_delivery_assignments (
    id                    BIGSERIAL       PRIMARY KEY,
    order_id              BIGINT          NOT NULL REFERENCES food_orders(id) ON DELETE CASCADE,
    partner_id            BIGINT          NOT NULL REFERENCES food_delivery_partners(id),
    status                VARCHAR(30)     NOT NULL DEFAULT 'ASSIGNED',
    assigned_at           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at           TIMESTAMP,
    picked_up_at          TIMESTAMP,
    delivered_at          TIMESTAMP,
    distance_km           DECIMAL(8,2),
    delivery_fee          DECIMAL(10,2),
    tip                   DECIMAL(10,2)   DEFAULT 0,
    otp_code              VARCHAR(6),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_del_assign_status CHECK (status IN ('ASSIGNED','ACCEPTED','PICKED_UP','IN_TRANSIT','DELIVERED','CANCELLED'))
);

CREATE TABLE IF NOT EXISTS food_delivery_zones (
    id                    BIGSERIAL       PRIMARY KEY,
    zone_name             VARCHAR(150)    NOT NULL,
    zone_type             VARCHAR(30)     NOT NULL DEFAULT 'COMMUNITY',
    polygon               JSONB,
    base_delivery_fee     DECIMAL(10,2)   DEFAULT 0,
    per_km_fee            DECIMAL(10,2)   DEFAULT 0,
    surge_multiplier      DECIMAL(5,2)    DEFAULT 1.00,
    active                BOOLEAN         NOT NULL DEFAULT TRUE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_del_zone_type CHECK (zone_type IN ('COMMUNITY','AREA','CITY'))
);

CREATE TABLE IF NOT EXISTS food_delivery_lockers (
    id                    BIGSERIAL       PRIMARY KEY,
    location_name         VARCHAR(200)    NOT NULL,
    locker_code           VARCHAR(30),
    capacity              INT             NOT NULL DEFAULT 10,
    available             INT             NOT NULL DEFAULT 10,
    temperature_controlled BOOLEAN        NOT NULL DEFAULT FALSE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_delivery_locker_assignments (
    id                    BIGSERIAL       PRIMARY KEY,
    locker_id             BIGINT          NOT NULL REFERENCES food_delivery_lockers(id) ON DELETE CASCADE,
    order_id              BIGINT          NOT NULL REFERENCES food_orders(id) ON DELETE CASCADE,
    compartment_number    VARCHAR(10),
    access_code           VARCHAR(20),
    assigned_at           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    picked_up_at          TIMESTAMP,
    expires_at            TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_delivery_route_logs (
    id                    BIGSERIAL       PRIMARY KEY,
    assignment_id         BIGINT          NOT NULL REFERENCES food_delivery_assignments(id) ON DELETE CASCADE,
    latitude              DECIMAL(10,7)   NOT NULL,
    longitude             DECIMAL(10,7)   NOT NULL,
    speed                 DECIMAL(6,2),
    timestamp             TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =====================================================================================
-- 10. GROCERY MARKETPLACE ENGINE (7 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_grocery_stores (
    id                    BIGSERIAL       PRIMARY KEY,
    name                  VARCHAR(200)    NOT NULL,
    slug                  VARCHAR(200),
    description           VARCHAR(2000),
    address               VARCHAR(500),
    logo_url              VARCHAR(500),
    cover_image_url       VARCHAR(500),
    store_type            VARCHAR(30)     NOT NULL DEFAULT 'SUPERMARKET',
    status                VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    rating                DECIMAL(3,2)    DEFAULT 0.00,
    delivery_enabled      BOOLEAN         NOT NULL DEFAULT TRUE,
    min_order             DECIMAL(10,2)   DEFAULT 0,
    delivery_fee          DECIMAL(10,2)   DEFAULT 0,
    owner_id              BIGINT          REFERENCES app_user(id),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_food_grocery_store_type CHECK (store_type IN ('SUPERMARKET','ORGANIC','DAIRY','BAKERY','MEAT','SEAFOOD'))
);

CREATE TABLE IF NOT EXISTS food_grocery_categories (
    id                    BIGSERIAL       PRIMARY KEY,
    store_id              BIGINT          REFERENCES food_grocery_stores(id) ON DELETE CASCADE,
    name                  VARCHAR(150)    NOT NULL,
    slug                  VARCHAR(150),
    icon                  VARCHAR(100),
    parent_id             BIGINT          REFERENCES food_grocery_categories(id),
    sort_order            INT             NOT NULL DEFAULT 0,
    active                BOOLEAN         NOT NULL DEFAULT TRUE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_grocery_products (
    id                    BIGSERIAL       PRIMARY KEY,
    store_id              BIGINT          NOT NULL REFERENCES food_grocery_stores(id) ON DELETE CASCADE,
    category_id           BIGINT          REFERENCES food_grocery_categories(id) ON DELETE SET NULL,
    name                  VARCHAR(200)    NOT NULL,
    slug                  VARCHAR(200),
    description           VARCHAR(1000),
    image_url             VARCHAR(500),
    images                TEXT[],
    brand                 VARCHAR(100),
    unit                  VARCHAR(30),
    unit_value            DECIMAL(10,2),
    price                 DECIMAL(10,2)   NOT NULL,
    discounted_price      DECIMAL(10,2),
    stock                 INT             DEFAULT 0,
    low_stock_threshold   INT             DEFAULT 5,
    barcode               VARCHAR(50),
    is_organic            BOOLEAN         NOT NULL DEFAULT FALSE,
    is_featured           BOOLEAN         NOT NULL DEFAULT FALSE,
    nutritional_info      JSONB,
    active                BOOLEAN         NOT NULL DEFAULT TRUE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_food_grocery_product_price CHECK (price >= 0)
);

CREATE TABLE IF NOT EXISTS food_grocery_orders (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    store_id              BIGINT          NOT NULL REFERENCES food_grocery_stores(id),
    order_number          VARCHAR(50)     NOT NULL UNIQUE,
    status                VARCHAR(30)     NOT NULL DEFAULT 'PLACED',
    subtotal              DECIMAL(12,2)   NOT NULL,
    tax                   DECIMAL(10,2)   DEFAULT 0,
    delivery_fee          DECIMAL(10,2)   DEFAULT 0,
    discount              DECIMAL(10,2)   DEFAULT 0,
    total_amount          DECIMAL(12,2)   NOT NULL,
    delivery_address      TEXT,
    delivery_slot         TIMESTAMP,
    delivered_at          TIMESTAMP,
    payment_status        VARCHAR(30)     DEFAULT 'PENDING',
    payment_method        VARCHAR(50),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_grocery_order_amt CHECK (total_amount >= 0)
);

CREATE TABLE IF NOT EXISTS food_grocery_order_items (
    id                    BIGSERIAL       PRIMARY KEY,
    order_id              BIGINT          NOT NULL REFERENCES food_grocery_orders(id) ON DELETE CASCADE,
    product_id            BIGINT          REFERENCES food_grocery_products(id),
    product_name          VARCHAR(200)    NOT NULL,
    quantity              INT             NOT NULL DEFAULT 1,
    unit_price            DECIMAL(10,2)   NOT NULL,
    total_price           DECIMAL(10,2)   NOT NULL,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_grocery_item_qty CHECK (quantity > 0),
    CONSTRAINT chk_food_grocery_item_price CHECK (unit_price >= 0)
);

CREATE TABLE IF NOT EXISTS food_grocery_delivery_slots (
    id                    BIGSERIAL       PRIMARY KEY,
    store_id              BIGINT          NOT NULL REFERENCES food_grocery_stores(id) ON DELETE CASCADE,
    date                  DATE            NOT NULL,
    start_time            TIME            NOT NULL,
    end_time              TIME            NOT NULL,
    capacity              INT             NOT NULL,
    booked                INT             DEFAULT 0,
    slot_type             VARCHAR(20)     NOT NULL DEFAULT 'STANDARD',
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_grocery_slot_type CHECK (slot_type IN ('STANDARD','EXPRESS','SCHEDULED'))
);

CREATE TABLE IF NOT EXISTS food_grocery_wishlists (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    product_id            BIGINT          NOT NULL REFERENCES food_grocery_products(id) ON DELETE CASCADE,
    added_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =====================================================================================
-- 11. KITCHEN INVENTORY ENGINE (7 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_kitchen_inventory (
    id                    BIGSERIAL       PRIMARY KEY,
    kitchen_type          VARCHAR(30)     NOT NULL,
    kitchen_id            BIGINT          NOT NULL,
    item_name             VARCHAR(200)    NOT NULL,
    category              VARCHAR(100),
    unit                  VARCHAR(30),
    current_stock         DECIMAL(12,2)   DEFAULT 0,
    min_stock             DECIMAL(12,2)   DEFAULT 0,
    max_stock             DECIMAL(12,2),
    reorder_level         DECIMAL(12,2),
    unit_cost             DECIMAL(10,2)   DEFAULT 0,
    last_restocked_at     TIMESTAMP,
    expiry_date           DATE,
    storage_location      VARCHAR(100),
    barcode               VARCHAR(50),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_inventory_kitchen_type CHECK (kitchen_type IN ('RESTAURANT','HOME_CHEF','CLOUD_KITCHEN','COMMUNITY_KITCHEN'))
);

CREATE TABLE IF NOT EXISTS food_kitchen_inventory_transactions (
    id                    BIGSERIAL       PRIMARY KEY,
    inventory_id          BIGINT          NOT NULL REFERENCES food_kitchen_inventory(id) ON DELETE CASCADE,
    transaction_type      VARCHAR(30)     NOT NULL,
    quantity              DECIMAL(12,2)   NOT NULL,
    unit_cost             DECIMAL(10,2),
    reference_type        VARCHAR(50),
    reference_id          BIGINT,
    notes                 VARCHAR(500),
    performed_by          BIGINT          REFERENCES app_user(id),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_inv_txn_type CHECK (transaction_type IN ('PURCHASE','CONSUMPTION','WASTAGE','RETURN','ADJUSTMENT'))
);

CREATE TABLE IF NOT EXISTS food_kitchen_suppliers (
    id                    BIGSERIAL       PRIMARY KEY,
    name                  VARCHAR(200)    NOT NULL,
    contact_person        VARCHAR(150),
    email                 VARCHAR(150),
    phone                 VARCHAR(20),
    address               VARCHAR(500),
    gst_number            VARCHAR(30),
    rating                DECIMAL(3,2)    DEFAULT 0.00,
    payment_terms         VARCHAR(200),
    active                BOOLEAN         NOT NULL DEFAULT TRUE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS food_kitchen_purchase_orders (
    id                    BIGSERIAL       PRIMARY KEY,
    kitchen_type          VARCHAR(30)     NOT NULL,
    kitchen_id            BIGINT          NOT NULL,
    supplier_id           BIGINT          NOT NULL REFERENCES food_kitchen_suppliers(id),
    order_number          VARCHAR(50)     NOT NULL UNIQUE,
    status                VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    total_amount          DECIMAL(14,2)   DEFAULT 0,
    expected_delivery     DATE,
    approved_by           BIGINT          REFERENCES app_user(id),
    notes                 VARCHAR(1000),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_po_status CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','RECEIVED','CANCELLED')),
    CONSTRAINT chk_food_po_amt CHECK (total_amount >= 0)
);

CREATE TABLE IF NOT EXISTS food_kitchen_purchase_order_items (
    id                    BIGSERIAL       PRIMARY KEY,
    order_id              BIGINT          NOT NULL REFERENCES food_kitchen_purchase_orders(id) ON DELETE CASCADE,
    inventory_id          BIGINT          REFERENCES food_kitchen_inventory(id),
    quantity              DECIMAL(12,2)   NOT NULL,
    unit_cost             DECIMAL(10,2)   NOT NULL,
    received_quantity     DECIMAL(12,2)   DEFAULT 0,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_po_item_qty CHECK (quantity > 0),
    CONSTRAINT chk_food_po_item_cost CHECK (unit_cost >= 0)
);

CREATE TABLE IF NOT EXISTS food_kitchen_waste_logs (
    id                    BIGSERIAL       PRIMARY KEY,
    kitchen_type          VARCHAR(30)     NOT NULL,
    kitchen_id            BIGINT          NOT NULL,
    date                  DATE            NOT NULL,
    item_name             VARCHAR(200)    NOT NULL,
    quantity              DECIMAL(12,2)   NOT NULL,
    unit                  VARCHAR(30),
    reason                VARCHAR(30)     NOT NULL,
    cost_impact           DECIMAL(10,2)   DEFAULT 0,
    logged_by             BIGINT          REFERENCES app_user(id),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_waste_reason CHECK (reason IN ('EXPIRED','SPOILED','OVERPRODUCTION','DAMAGED'))
);

CREATE TABLE IF NOT EXISTS food_kitchen_inventory_forecasts (
    id                    BIGSERIAL       PRIMARY KEY,
    kitchen_type          VARCHAR(30)     NOT NULL,
    kitchen_id            BIGINT          NOT NULL,
    item_name             VARCHAR(200)    NOT NULL,
    forecast_date         DATE            NOT NULL,
    predicted_demand      DECIMAL(12,2),
    confidence            DECIMAL(5,2),
    generated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_inv_forecast_conf CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 100)
);


-- =====================================================================================
-- 12. RECIPE ENGINE (6 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_recipes (
    id                    BIGSERIAL       PRIMARY KEY,
    title                 VARCHAR(200)    NOT NULL,
    slug                  VARCHAR(200),
    description           VARCHAR(2000),
    cuisine_type          VARCHAR(100),
    meal_type             VARCHAR(30),
    course_type           VARCHAR(30),
    difficulty            VARCHAR(20)     DEFAULT 'MEDIUM',
    prep_time             INT,
    cook_time             INT,
    total_time            INT,
    servings              INT,
    calories              INT,
    protein               DECIMAL(8,2),
    carbs                 DECIMAL(8,2),
    fat                   DECIMAL(8,2),
    image_url             VARCHAR(500),
    video_url             VARCHAR(500),
    instructions          JSONB,
    tips                  TEXT,
    tags                  TEXT[],
    is_veg                BOOLEAN         NOT NULL DEFAULT FALSE,
    is_vegan              BOOLEAN         NOT NULL DEFAULT FALSE,
    is_gluten_free        BOOLEAN         NOT NULL DEFAULT FALSE,
    author_id             BIGINT          REFERENCES app_user(id),
    source_type           VARCHAR(20)     DEFAULT 'COMMUNITY',
    status                VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    view_count            INT             DEFAULT 0,
    like_count            INT             DEFAULT 0,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_food_recipe_course CHECK (course_type IS NULL OR course_type IN ('APPETIZER','MAIN','DESSERT','BEVERAGE','SNACK')),
    CONSTRAINT chk_food_recipe_difficulty CHECK (difficulty IS NULL OR difficulty IN ('EASY','MEDIUM','HARD')),
    CONSTRAINT chk_food_recipe_source CHECK (source_type IS NULL OR source_type IN ('COMMUNITY','SYSTEM','AI')),
    CONSTRAINT chk_food_recipe_status CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED'))
);

CREATE TABLE IF NOT EXISTS food_recipe_ingredients (
    id                    BIGSERIAL       PRIMARY KEY,
    recipe_id             BIGINT          NOT NULL REFERENCES food_recipes(id) ON DELETE CASCADE,
    ingredient_name       VARCHAR(150)    NOT NULL,
    quantity              DECIMAL(10,2),
    unit                  VARCHAR(30),
    is_optional           BOOLEAN         NOT NULL DEFAULT FALSE,
    substitute            VARCHAR(200),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_recipe_collections (
    id                    BIGSERIAL       PRIMARY KEY,
    name                  VARCHAR(200)    NOT NULL,
    description           VARCHAR(1000),
    image_url             VARCHAR(500),
    user_id               BIGINT          REFERENCES app_user(id),
    is_public             BOOLEAN         NOT NULL DEFAULT TRUE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_recipe_collection_items (
    id                    BIGSERIAL       PRIMARY KEY,
    collection_id         BIGINT          NOT NULL REFERENCES food_recipe_collections(id) ON DELETE CASCADE,
    recipe_id             BIGINT          NOT NULL REFERENCES food_recipes(id) ON DELETE CASCADE,
    sort_order            INT             NOT NULL DEFAULT 0,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_recipe_comments (
    id                    BIGSERIAL       PRIMARY KEY,
    recipe_id             BIGINT          NOT NULL REFERENCES food_recipes(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    comment_text          VARCHAR(2000)   NOT NULL,
    parent_id             BIGINT          REFERENCES food_recipe_comments(id),
    likes                 INT             DEFAULT 0,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS food_recipe_ratings (
    id                    BIGSERIAL       PRIMARY KEY,
    recipe_id             BIGINT          NOT NULL REFERENCES food_recipes(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    rating                INT             NOT NULL,
    tried_it              BOOLEAN         NOT NULL DEFAULT FALSE,
    review_text           VARCHAR(2000),
    images                TEXT[],
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_recipe_rating CHECK (rating BETWEEN 1 AND 5)
);


-- =====================================================================================
-- 13. NUTRITION ENGINE (7 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_nutritionists (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    qualification         VARCHAR(200),
    specialization        VARCHAR(200),
    license_number        VARCHAR(50),
    experience_years      INT,
    bio                   TEXT,
    consultation_fee      DECIMAL(10,2),
    rating                DECIMAL(3,2)    DEFAULT 0.00,
    status                VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    profile_image_url     VARCHAR(500),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_food_nutritionist_fee CHECK (consultation_fee IS NULL OR consultation_fee >= 0)
);

CREATE TABLE IF NOT EXISTS food_nutrition_consultations (
    id                    BIGSERIAL       PRIMARY KEY,
    nutritionist_id       BIGINT          NOT NULL REFERENCES food_nutritionists(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    scheduled_at          TIMESTAMP       NOT NULL,
    duration_minutes      INT             DEFAULT 30,
    status                VARCHAR(30)     NOT NULL DEFAULT 'SCHEDULED',
    consultation_type     VARCHAR(20)     NOT NULL DEFAULT 'VIDEO',
    notes                 TEXT,
    follow_up_date        DATE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_consult_status CHECK (status IN ('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED')),
    CONSTRAINT chk_food_consult_type CHECK (consultation_type IN ('IN_PERSON','VIDEO','CHAT'))
);

CREATE TABLE IF NOT EXISTS food_meal_plans (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    nutritionist_id       BIGINT          REFERENCES food_nutritionists(id),
    name                  VARCHAR(200)    NOT NULL,
    description           VARCHAR(1000),
    start_date            DATE            NOT NULL,
    end_date              DATE            NOT NULL,
    goal                  VARCHAR(200),
    daily_calories        INT,
    daily_protein         DECIMAL(8,2),
    status                VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_meal_plan_status CHECK (status IN ('ACTIVE','COMPLETED','PAUSED'))
);

CREATE TABLE IF NOT EXISTS food_meal_plan_items (
    id                    BIGSERIAL       PRIMARY KEY,
    plan_id               BIGINT          NOT NULL REFERENCES food_meal_plans(id) ON DELETE CASCADE,
    day_of_week           INT             NOT NULL,
    meal_type             VARCHAR(20)     NOT NULL,
    recipe_id             BIGINT          REFERENCES food_recipes(id),
    item_name             VARCHAR(200),
    calories              INT,
    protein               DECIMAL(8,2),
    carbs                 DECIMAL(8,2),
    fat                   DECIMAL(8,2),
    portion_size          VARCHAR(100),
    notes                 VARCHAR(500),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_meal_plan_item_dow CHECK (day_of_week BETWEEN 0 AND 6)
);

CREATE TABLE IF NOT EXISTS food_calorie_logs (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    date                  DATE            NOT NULL,
    meal_type             VARCHAR(20)     NOT NULL,
    item_name             VARCHAR(200)    NOT NULL,
    calories              INT,
    protein               DECIMAL(8,2),
    carbs                 DECIMAL(8,2),
    fat                   DECIMAL(8,2),
    fiber                 DECIMAL(8,2),
    quantity              DECIMAL(10,2),
    unit                  VARCHAR(30),
    source                VARCHAR(20)     DEFAULT 'MANUAL',
    reference_id          BIGINT,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_calorie_source CHECK (source IS NULL OR source IN ('MANUAL','SCAN','ORDER','RECIPE'))
);

CREATE TABLE IF NOT EXISTS food_weight_logs (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    date                  DATE            NOT NULL,
    weight                DECIMAL(6,2)    NOT NULL,
    unit                  VARCHAR(5)      NOT NULL DEFAULT 'KG',
    body_fat_pct          DECIMAL(5,2),
    muscle_mass           DECIMAL(6,2),
    notes                 VARCHAR(500),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_weight_unit CHECK (unit IN ('KG','LBS')),
    CONSTRAINT chk_food_weight_bf CHECK (body_fat_pct IS NULL OR body_fat_pct BETWEEN 0 AND 100)
);

CREATE TABLE IF NOT EXISTS food_water_logs (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    date                  DATE            NOT NULL,
    intake_ml             INT             NOT NULL DEFAULT 0,
    goal_ml               INT,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_water_intake CHECK (intake_ml >= 0)
);


-- =====================================================================================
-- 14. FOOD EVENTS ENGINE (5 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_events (
    id                    BIGSERIAL       PRIMARY KEY,
    name                  VARCHAR(200)    NOT NULL,
    description           VARCHAR(2000),
    event_type            VARCHAR(30)     NOT NULL,
    venue                 VARCHAR(300),
    date                  DATE            NOT NULL,
    start_time            TIME,
    end_time              TIME,
    capacity              INT,
    registered            INT             DEFAULT 0,
    price                 DECIMAL(10,2)   DEFAULT 0,
    image_url             VARCHAR(500),
    organizer_id          BIGINT          REFERENCES app_user(id),
    status                VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    registration_deadline TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_food_event_type CHECK (event_type IN ('POTLUCK','FESTIVAL','COOKING_COMPETITION','WINE_TASTING','BBQ','KIDS_COOKING','RECIPE_CONTEST','FOOD_EXHIBITION')),
    CONSTRAINT chk_food_event_status CHECK (status IN ('DRAFT','PUBLISHED','ONGOING','COMPLETED','CANCELLED')),
    CONSTRAINT chk_food_event_price CHECK (price >= 0)
);

CREATE TABLE IF NOT EXISTS food_event_registrations (
    id                    BIGSERIAL       PRIMARY KEY,
    event_id              BIGINT          NOT NULL REFERENCES food_events(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    guests                INT             DEFAULT 0,
    total_amount          DECIMAL(10,2)   DEFAULT 0,
    status                VARCHAR(30)     NOT NULL DEFAULT 'REGISTERED',
    qr_code               VARCHAR(500),
    checked_in_at         TIMESTAMP,
    dietary_requirements  TEXT,
    contribution_item     VARCHAR(200),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_event_reg_status CHECK (status IN ('REGISTERED','CONFIRMED','ATTENDED','CANCELLED'))
);

CREATE TABLE IF NOT EXISTS food_event_contributions (
    id                    BIGSERIAL       PRIMARY KEY,
    event_id              BIGINT          NOT NULL REFERENCES food_events(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    item_name             VARCHAR(200)    NOT NULL,
    item_type             VARCHAR(30)     NOT NULL,
    quantity              VARCHAR(100),
    serving_size          VARCHAR(100),
    is_veg                BOOLEAN         NOT NULL DEFAULT FALSE,
    allergens             TEXT,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_event_contrib_type CHECK (item_type IN ('FOOD','BEVERAGE','DESSERT','EQUIPMENT'))
);

CREATE TABLE IF NOT EXISTS food_event_feedback (
    id                    BIGSERIAL       PRIMARY KEY,
    event_id              BIGINT          NOT NULL REFERENCES food_events(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    rating                INT             NOT NULL,
    food_rating           INT,
    organization_rating   INT,
    venue_rating          INT,
    comments              VARCHAR(2000),
    suggestions           VARCHAR(1000),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_event_fb_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_food_event_fb_food CHECK (food_rating IS NULL OR food_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_food_event_fb_org CHECK (organization_rating IS NULL OR organization_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_food_event_fb_venue CHECK (venue_rating IS NULL OR venue_rating BETWEEN 1 AND 5)
);

CREATE TABLE IF NOT EXISTS food_event_sponsors (
    id                    BIGSERIAL       PRIMARY KEY,
    event_id              BIGINT          NOT NULL REFERENCES food_events(id) ON DELETE CASCADE,
    sponsor_name          VARCHAR(200)    NOT NULL,
    logo_url              VARCHAR(500),
    contribution_type     VARCHAR(100),
    amount                DECIMAL(12,2)   DEFAULT 0,
    active                BOOLEAN         NOT NULL DEFAULT TRUE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =====================================================================================
-- 15. CORPORATE FOOD ENGINE (6 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_corporate_accounts (
    id                    BIGSERIAL       PRIMARY KEY,
    company_name          VARCHAR(200)    NOT NULL,
    billing_address       VARCHAR(500),
    contact_person        VARCHAR(150),
    contact_email         VARCHAR(150),
    contact_phone         VARCHAR(20),
    gst_number            VARCHAR(30),
    credit_limit          DECIMAL(14,2)   DEFAULT 0,
    balance               DECIMAL(14,2)   DEFAULT 0,
    status                VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS food_corporate_meal_cards (
    id                    BIGSERIAL       PRIMARY KEY,
    account_id            BIGINT          NOT NULL REFERENCES food_corporate_accounts(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    card_number           VARCHAR(50)     NOT NULL UNIQUE,
    daily_limit           DECIMAL(10,2)   DEFAULT 0,
    monthly_limit         DECIMAL(12,2)   DEFAULT 0,
    balance               DECIMAL(12,2)   DEFAULT 0,
    status                VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    valid_from            DATE,
    valid_until           DATE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_meal_card_status CHECK (status IN ('ACTIVE','SUSPENDED','EXPIRED'))
);

CREATE TABLE IF NOT EXISTS food_corporate_meal_card_transactions (
    id                    BIGSERIAL       PRIMARY KEY,
    card_id               BIGINT          NOT NULL REFERENCES food_corporate_meal_cards(id) ON DELETE CASCADE,
    order_id              BIGINT,
    amount                DECIMAL(10,2)   NOT NULL,
    transaction_type      VARCHAR(20)     NOT NULL,
    balance_after         DECIMAL(12,2)   NOT NULL,
    description           VARCHAR(300),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_meal_card_txn_type CHECK (transaction_type IN ('DEBIT','CREDIT','REFUND'))
);

CREATE TABLE IF NOT EXISTS food_corporate_cafeterias (
    id                    BIGSERIAL       PRIMARY KEY,
    account_id            BIGINT          NOT NULL REFERENCES food_corporate_accounts(id) ON DELETE CASCADE,
    name                  VARCHAR(200)    NOT NULL,
    location              VARCHAR(500),
    capacity              INT,
    manager_id            BIGINT          REFERENCES app_user(id),
    status                VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_corporate_cafeteria_menus (
    id                    BIGSERIAL       PRIMARY KEY,
    cafeteria_id          BIGINT          NOT NULL REFERENCES food_corporate_cafeterias(id) ON DELETE CASCADE,
    date                  DATE            NOT NULL,
    meal_type             VARCHAR(20)     NOT NULL,
    items                 JSONB,
    price                 DECIMAL(10,2)   DEFAULT 0,
    total_plates          INT,
    booked_plates         INT             DEFAULT 0,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_corporate_catering_requests (
    id                    BIGSERIAL       PRIMARY KEY,
    account_id            BIGINT          NOT NULL REFERENCES food_corporate_accounts(id) ON DELETE CASCADE,
    requested_by          BIGINT          NOT NULL REFERENCES app_user(id),
    event_name            VARCHAR(200),
    event_date            DATE            NOT NULL,
    event_time            TIME,
    guest_count           INT             NOT NULL,
    budget                DECIMAL(14,2),
    menu_preferences      TEXT,
    dietary_requirements  TEXT,
    venue                 VARCHAR(300),
    status                VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    notes                 VARCHAR(1000),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_corp_catering_status CHECK (status IN ('PENDING','QUOTED','APPROVED','CONFIRMED','COMPLETED','CANCELLED'))
);


-- =====================================================================================
-- 16. CATERING ENGINE (6 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_caterers (
    id                    BIGSERIAL       PRIMARY KEY,
    name                  VARCHAR(200)    NOT NULL,
    description           VARCHAR(2000),
    cuisine_types         TEXT[],
    min_order_count       INT             DEFAULT 10,
    max_order_count       INT,
    price_per_plate_from  DECIMAL(10,2),
    price_per_plate_to    DECIMAL(10,2),
    fssai_license         VARCHAR(50),
    rating                DECIMAL(3,2)    DEFAULT 0.00,
    total_events          INT             DEFAULT 0,
    status                VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    logo_url              VARCHAR(500),
    user_id               BIGINT          REFERENCES app_user(id),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS food_catering_packages (
    id                    BIGSERIAL       PRIMARY KEY,
    caterer_id            BIGINT          NOT NULL REFERENCES food_caterers(id) ON DELETE CASCADE,
    name                  VARCHAR(200)    NOT NULL,
    description           VARCHAR(1000),
    occasion_type         VARCHAR(30)     NOT NULL,
    items_per_plate       INT,
    price_per_plate       DECIMAL(10,2)   NOT NULL,
    min_plates            INT             DEFAULT 10,
    includes              TEXT[],
    image_url             VARCHAR(500),
    active                BOOLEAN         NOT NULL DEFAULT TRUE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_catering_pkg_occasion CHECK (occasion_type IN ('BIRTHDAY','WEDDING','HOUSEWARMING','CORPORATE','FESTIVAL','SPORTS','SCHOOL')),
    CONSTRAINT chk_food_catering_pkg_price CHECK (price_per_plate >= 0)
);

CREATE TABLE IF NOT EXISTS food_catering_requests (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    occasion_type         VARCHAR(50),
    event_date            DATE            NOT NULL,
    event_time            TIME,
    venue                 VARCHAR(300),
    guest_count           INT             NOT NULL,
    budget                DECIMAL(14,2),
    menu_preferences      TEXT,
    dietary_requirements  TEXT,
    status                VARCHAR(30)     NOT NULL DEFAULT 'OPEN',
    selected_caterer_id   BIGINT          REFERENCES food_caterers(id),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_catering_req_status CHECK (status IN ('OPEN','QUOTED','AWARDED','IN_PROGRESS','COMPLETED','CANCELLED'))
);

CREATE TABLE IF NOT EXISTS food_catering_quotations (
    id                    BIGSERIAL       PRIMARY KEY,
    request_id            BIGINT          NOT NULL REFERENCES food_catering_requests(id) ON DELETE CASCADE,
    caterer_id            BIGINT          NOT NULL REFERENCES food_caterers(id),
    menu                  JSONB,
    price_per_plate       DECIMAL(10,2)   NOT NULL,
    total_amount          DECIMAL(14,2)   NOT NULL,
    valid_until           DATE,
    status                VARCHAR(30)     NOT NULL DEFAULT 'SUBMITTED',
    notes                 VARCHAR(1000),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_catering_quot_status CHECK (status IN ('SUBMITTED','ACCEPTED','REJECTED')),
    CONSTRAINT chk_food_catering_quot_amt CHECK (total_amount >= 0)
);

CREATE TABLE IF NOT EXISTS food_catering_orders (
    id                    BIGSERIAL       PRIMARY KEY,
    request_id            BIGINT          REFERENCES food_catering_requests(id),
    caterer_id            BIGINT          NOT NULL REFERENCES food_caterers(id),
    quotation_id          BIGINT          REFERENCES food_catering_quotations(id),
    order_number          VARCHAR(50)     NOT NULL UNIQUE,
    total_amount          DECIMAL(14,2)   NOT NULL,
    advance_amount        DECIMAL(14,2)   DEFAULT 0,
    balance_amount        DECIMAL(14,2)   DEFAULT 0,
    status                VARCHAR(30)     NOT NULL DEFAULT 'CONFIRMED',
    payment_status        VARCHAR(30)     DEFAULT 'PENDING',
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_catering_order_status CHECK (status IN ('CONFIRMED','PREPARING','DELIVERED','COMPLETED')),
    CONSTRAINT chk_food_catering_order_amt CHECK (total_amount >= 0)
);

CREATE TABLE IF NOT EXISTS food_catering_reviews (
    id                    BIGSERIAL       PRIMARY KEY,
    order_id              BIGINT          NOT NULL REFERENCES food_catering_orders(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    rating                INT             NOT NULL,
    food_rating           INT,
    service_rating        INT,
    presentation_rating   INT,
    review_text           VARCHAR(2000),
    images                TEXT[],
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_catering_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_food_catering_review_food CHECK (food_rating IS NULL OR food_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_food_catering_review_svc CHECK (service_rating IS NULL OR service_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_food_catering_review_pres CHECK (presentation_rating IS NULL OR presentation_rating BETWEEN 1 AND 5)
);


-- =====================================================================================
-- 17. AI RECOMMENDATION ENGINE (4 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_ai_recommendations (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    recommendation_type   VARCHAR(30)     NOT NULL,
    title                 VARCHAR(200),
    description           VARCHAR(1000),
    data                  JSONB,
    score                 DECIMAL(5,2),
    status                VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    generated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at            TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_ai_rec_type CHECK (recommendation_type IN ('RESTAURANT','MEAL','RECIPE','GROCERY','NUTRITION','BUDGET')),
    CONSTRAINT chk_food_ai_rec_status CHECK (status IN ('PENDING','ACCEPTED','DISMISSED'))
);

CREATE TABLE IF NOT EXISTS food_ai_meal_plans (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    week_start            DATE            NOT NULL,
    meals                 JSONB,
    total_calories        INT,
    total_cost            DECIMAL(12,2),
    generated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status                VARCHAR(20)     NOT NULL DEFAULT 'GENERATED',
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_ai_meal_status CHECK (status IN ('GENERATED','ACTIVE','MODIFIED','EXPIRED'))
);

CREATE TABLE IF NOT EXISTS food_ai_grocery_lists (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    generated_for_date    DATE            NOT NULL,
    items                 JSONB,
    estimated_cost        DECIMAL(12,2),
    status                VARCHAR(20)     NOT NULL DEFAULT 'GENERATED',
    generated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_ai_grocery_status CHECK (status IN ('GENERATED','ORDERED','COMPLETED'))
);

CREATE TABLE IF NOT EXISTS food_ai_demand_predictions (
    id                    BIGSERIAL       PRIMARY KEY,
    provider_type         VARCHAR(30)     NOT NULL,
    provider_id           BIGINT          NOT NULL,
    prediction_date       DATE            NOT NULL,
    predicted_orders      INT,
    predicted_revenue     DECIMAL(14,2),
    confidence            DECIMAL(5,2),
    factors               JSONB,
    generated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_ai_demand_conf CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 100)
);


-- =====================================================================================
-- 18. SMART PANTRY ENGINE (5 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_pantry_items (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    item_name             VARCHAR(200)    NOT NULL,
    category              VARCHAR(100),
    quantity              DECIMAL(10,2),
    unit                  VARCHAR(30),
    purchase_date         DATE,
    expiry_date           DATE,
    barcode               VARCHAR(50),
    image_url             VARCHAR(500),
    storage_location      VARCHAR(20)     DEFAULT 'PANTRY',
    status                VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE',
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_pantry_storage CHECK (storage_location IS NULL OR storage_location IN ('FRIDGE','FREEZER','PANTRY','COUNTER')),
    CONSTRAINT chk_food_pantry_status CHECK (status IN ('AVAILABLE','LOW','EXPIRED','CONSUMED'))
);

CREATE TABLE IF NOT EXISTS food_pantry_consumption_logs (
    id                    BIGSERIAL       PRIMARY KEY,
    pantry_item_id        BIGINT          NOT NULL REFERENCES food_pantry_items(id) ON DELETE CASCADE,
    quantity_used         DECIMAL(10,2)   NOT NULL,
    used_for              VARCHAR(200),
    logged_at             TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id               BIGINT          REFERENCES app_user(id),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_pantry_shopping_lists (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    name                  VARCHAR(200),
    status                VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_pantry_list_status CHECK (status IN ('ACTIVE','COMPLETED'))
);

CREATE TABLE IF NOT EXISTS food_pantry_shopping_list_items (
    id                    BIGSERIAL       PRIMARY KEY,
    list_id               BIGINT          NOT NULL REFERENCES food_pantry_shopping_lists(id) ON DELETE CASCADE,
    item_name             VARCHAR(200)    NOT NULL,
    category              VARCHAR(100),
    quantity              DECIMAL(10,2),
    unit                  VARCHAR(30),
    estimated_price       DECIMAL(10,2),
    is_purchased          BOOLEAN         NOT NULL DEFAULT FALSE,
    purchased_price       DECIMAL(10,2),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_pantry_alerts (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    pantry_item_id        BIGINT          REFERENCES food_pantry_items(id) ON DELETE CASCADE,
    alert_type            VARCHAR(30)     NOT NULL,
    message               VARCHAR(500),
    is_read               BOOLEAN         NOT NULL DEFAULT FALSE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_pantry_alert_type CHECK (alert_type IN ('EXPIRING_SOON','EXPIRED','LOW_STOCK','REORDER'))
);


-- =====================================================================================
-- 19. LOYALTY ENGINE (6 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_loyalty_programs (
    id                    BIGSERIAL       PRIMARY KEY,
    name                  VARCHAR(200)    NOT NULL,
    description           VARCHAR(1000),
    program_type          VARCHAR(20)     NOT NULL DEFAULT 'POINTS',
    points_per_rupee      DECIMAL(5,2)    DEFAULT 1.00,
    min_redeem_points     INT             DEFAULT 100,
    point_value           DECIMAL(5,2)    DEFAULT 0.25,
    status                VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_loyalty_prog_type CHECK (program_type IN ('POINTS','CASHBACK','TIER'))
);

CREATE TABLE IF NOT EXISTS food_loyalty_members (
    id                    BIGSERIAL       PRIMARY KEY,
    program_id            BIGINT          NOT NULL REFERENCES food_loyalty_programs(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    points_balance        INT             DEFAULT 0,
    lifetime_points       INT             DEFAULT 0,
    tier                  VARCHAR(20)     DEFAULT 'BRONZE',
    joined_at             TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_loyalty_tier CHECK (tier IN ('BRONZE','SILVER','GOLD','PLATINUM'))
);

CREATE TABLE IF NOT EXISTS food_loyalty_transactions (
    id                    BIGSERIAL       PRIMARY KEY,
    member_id             BIGINT          NOT NULL REFERENCES food_loyalty_members(id) ON DELETE CASCADE,
    transaction_type      VARCHAR(20)     NOT NULL,
    points                INT             NOT NULL,
    reference_type        VARCHAR(50),
    reference_id          BIGINT,
    description           VARCHAR(300),
    expires_at            TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_loyalty_txn_type CHECK (transaction_type IN ('EARN','REDEEM','EXPIRE','BONUS'))
);

CREATE TABLE IF NOT EXISTS food_loyalty_coupons (
    id                    BIGSERIAL       PRIMARY KEY,
    code                  VARCHAR(50)     NOT NULL UNIQUE,
    title                 VARCHAR(200)    NOT NULL,
    description           VARCHAR(1000),
    discount_type         VARCHAR(20)     NOT NULL,
    discount_value        DECIMAL(10,2)   NOT NULL,
    min_order             DECIMAL(10,2)   DEFAULT 0,
    max_discount          DECIMAL(10,2),
    valid_from            TIMESTAMP       NOT NULL,
    valid_until           TIMESTAMP       NOT NULL,
    usage_limit           INT,
    used_count            INT             DEFAULT 0,
    applicable_to         VARCHAR(30)     NOT NULL DEFAULT 'ALL',
    provider_id           BIGINT,
    active                BOOLEAN         NOT NULL DEFAULT TRUE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_coupon_disc_type CHECK (discount_type IN ('PERCENTAGE','FLAT')),
    CONSTRAINT chk_food_coupon_applicable CHECK (applicable_to IN ('ALL','RESTAURANT','HOME_CHEF','GROCERY')),
    CONSTRAINT chk_food_coupon_value CHECK (discount_value >= 0)
);

CREATE TABLE IF NOT EXISTS food_loyalty_coupon_usages (
    id                    BIGSERIAL       PRIMARY KEY,
    coupon_id             BIGINT          NOT NULL REFERENCES food_loyalty_coupons(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    order_id              BIGINT,
    discount_applied      DECIMAL(10,2)   NOT NULL,
    used_at               TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_loyalty_gift_cards (
    id                    BIGSERIAL       PRIMARY KEY,
    card_number           VARCHAR(50)     NOT NULL UNIQUE,
    balance               DECIMAL(12,2)   NOT NULL DEFAULT 0,
    original_amount       DECIMAL(12,2)   NOT NULL,
    purchased_by          BIGINT          REFERENCES app_user(id),
    gifted_to             BIGINT          REFERENCES app_user(id),
    status                VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    valid_until           DATE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_gift_card_status CHECK (status IN ('ACTIVE','USED','EXPIRED')),
    CONSTRAINT chk_food_gift_card_amt CHECK (original_amount >= 0),
    CONSTRAINT chk_food_gift_card_bal CHECK (balance >= 0)
);


-- =====================================================================================
-- 20. REVIEW ENGINE (3 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_reviews (
    id                    BIGSERIAL       PRIMARY KEY,
    entity_type           VARCHAR(30)     NOT NULL,
    entity_id             BIGINT          NOT NULL,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    rating                INT             NOT NULL,
    title                 VARCHAR(200),
    review_text           VARCHAR(2000),
    images                TEXT[],
    helpful_count         INT             DEFAULT 0,
    reported              BOOLEAN         NOT NULL DEFAULT FALSE,
    status                VARCHAR(20)     NOT NULL DEFAULT 'PUBLISHED',
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_food_review_entity CHECK (entity_type IN ('RESTAURANT','HOME_CHEF','RECIPE','GROCERY_STORE','CATERER','PRODUCT')),
    CONSTRAINT chk_food_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_food_review_status CHECK (status IN ('PUBLISHED','HIDDEN','REMOVED'))
);

CREATE TABLE IF NOT EXISTS food_review_responses (
    id                    BIGSERIAL       PRIMARY KEY,
    review_id             BIGINT          NOT NULL REFERENCES food_reviews(id) ON DELETE CASCADE,
    responder_id          BIGINT          NOT NULL REFERENCES app_user(id),
    response_text         VARCHAR(2000)   NOT NULL,
    responded_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_review_reports (
    id                    BIGSERIAL       PRIMARY KEY,
    review_id             BIGINT          NOT NULL REFERENCES food_reviews(id) ON DELETE CASCADE,
    reporter_id           BIGINT          NOT NULL REFERENCES app_user(id),
    reason                VARCHAR(500),
    status                VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_review_report_status CHECK (status IN ('PENDING','REVIEWED','ACTION_TAKEN','DISMISSED'))
);


-- =====================================================================================
-- 21. PAYMENT ENGINE (3 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_payments (
    id                    BIGSERIAL       PRIMARY KEY,
    order_type            VARCHAR(30)     NOT NULL,
    order_id              BIGINT          NOT NULL,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    amount                DECIMAL(14,2)   NOT NULL,
    payment_method        VARCHAR(30)     NOT NULL,
    payment_gateway       VARCHAR(50),
    transaction_id        VARCHAR(100),
    status                VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    gateway_response      JSONB,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_payment_order_type CHECK (order_type IN ('FOOD_ORDER','GROCERY','SUBSCRIPTION','CATERING','DINING','EVENT','MEAL_CARD')),
    CONSTRAINT chk_food_payment_method CHECK (payment_method IN ('CARD','UPI','WALLET','NET_BANKING','COD','MEAL_CARD','COMMUNITY_WALLET')),
    CONSTRAINT chk_food_payment_status CHECK (status IN ('PENDING','SUCCESS','FAILED','REFUNDED')),
    CONSTRAINT chk_food_payment_amt CHECK (amount >= 0)
);

CREATE TABLE IF NOT EXISTS food_wallets (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    balance               DECIMAL(14,2)   NOT NULL DEFAULT 0,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_wallet_balance CHECK (balance >= 0)
);

CREATE TABLE IF NOT EXISTS food_wallet_transactions (
    id                    BIGSERIAL       PRIMARY KEY,
    wallet_id             BIGINT          NOT NULL REFERENCES food_wallets(id) ON DELETE CASCADE,
    transaction_type      VARCHAR(20)     NOT NULL,
    amount                DECIMAL(14,2)   NOT NULL,
    reference_type        VARCHAR(50),
    reference_id          BIGINT,
    description           VARCHAR(300),
    balance_after         DECIMAL(14,2)   NOT NULL,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_wallet_txn_type CHECK (transaction_type IN ('CREDIT','DEBIT','REFUND','CASHBACK')),
    CONSTRAINT chk_food_wallet_txn_amt CHECK (amount >= 0)
);


-- =====================================================================================
-- 22. NOTIFICATION ENGINE (2 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_notification_preferences (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    order_updates         BOOLEAN         NOT NULL DEFAULT TRUE,
    promotions            BOOLEAN         NOT NULL DEFAULT TRUE,
    subscription_reminders BOOLEAN        NOT NULL DEFAULT TRUE,
    expiry_alerts         BOOLEAN         NOT NULL DEFAULT TRUE,
    event_notifications   BOOLEAN         NOT NULL DEFAULT TRUE,
    nutrition_reminders   BOOLEAN         NOT NULL DEFAULT TRUE,
    delivery_updates      BOOLEAN         NOT NULL DEFAULT TRUE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_notification_logs (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    notification_type     VARCHAR(50)     NOT NULL,
    channel               VARCHAR(20)     NOT NULL,
    title                 VARCHAR(200),
    message               VARCHAR(1000),
    reference_type        VARCHAR(50),
    reference_id          BIGINT,
    status                VARCHAR(20)     NOT NULL DEFAULT 'SENT',
    sent_at               TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_notif_channel CHECK (channel IN ('PUSH','EMAIL','SMS','IN_APP')),
    CONSTRAINT chk_food_notif_status CHECK (status IN ('SENT','DELIVERED','READ','FAILED'))
);


-- =====================================================================================
-- 23. WORKFLOW ENGINE (2 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_workflow_definitions (
    id                    BIGSERIAL       PRIMARY KEY,
    name                  VARCHAR(200)    NOT NULL,
    entity_type           VARCHAR(50)     NOT NULL,
    trigger_event         VARCHAR(100),
    steps                 JSONB,
    active                BOOLEAN         NOT NULL DEFAULT TRUE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_workflow_instances (
    id                    BIGSERIAL       PRIMARY KEY,
    definition_id         BIGINT          NOT NULL REFERENCES food_workflow_definitions(id) ON DELETE CASCADE,
    entity_type           VARCHAR(50)     NOT NULL,
    entity_id             BIGINT          NOT NULL,
    current_step          INT             NOT NULL DEFAULT 0,
    status                VARCHAR(30)     NOT NULL DEFAULT 'IN_PROGRESS',
    started_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at          TIMESTAMP,
    data                  JSONB,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_workflow_status CHECK (status IN ('IN_PROGRESS','COMPLETED','FAILED','CANCELLED'))
);


-- =====================================================================================
-- 24. ANALYTICS ENGINE (3 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_analytics_daily (
    id                    BIGSERIAL       PRIMARY KEY,
    entity_type           VARCHAR(50)     NOT NULL,
    entity_id             BIGINT          NOT NULL,
    date                  DATE            NOT NULL,
    metric_name           VARCHAR(100)    NOT NULL,
    metric_value          DECIMAL(14,2),
    dimensions            JSONB,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_analytics_consumption_trends (
    id                    BIGSERIAL       PRIMARY KEY,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    month                 DATE            NOT NULL,
    total_orders          INT             DEFAULT 0,
    total_revenue         DECIMAL(14,2)   DEFAULT 0,
    avg_order_value       DECIMAL(10,2)   DEFAULT 0,
    top_cuisines          JSONB,
    top_restaurants       JSONB,
    veg_pct               DECIMAL(5,2)    DEFAULT 0,
    non_veg_pct           DECIMAL(5,2)    DEFAULT 0,
    subscription_count    INT             DEFAULT 0,
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_trends_veg CHECK (veg_pct BETWEEN 0 AND 100),
    CONSTRAINT chk_food_trends_nonveg CHECK (non_veg_pct BETWEEN 0 AND 100)
);

CREATE TABLE IF NOT EXISTS food_analytics_food_waste (
    id                    BIGSERIAL       PRIMARY KEY,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    month                 DATE            NOT NULL,
    total_waste_kg        DECIMAL(12,2)   DEFAULT 0,
    waste_by_source       JSONB,
    cost_impact           DECIMAL(12,2)   DEFAULT 0,
    reduction_pct         DECIMAL(5,2)    DEFAULT 0,
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_waste_analytics_red CHECK (reduction_pct BETWEEN 0 AND 100)
);


-- =====================================================================================
-- 25. FOOD SAFETY & COMPLIANCE ENGINE (4 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_safety_inspections (
    id                    BIGSERIAL       PRIMARY KEY,
    entity_type           VARCHAR(30)     NOT NULL,
    entity_id             BIGINT          NOT NULL,
    inspector_id          BIGINT          REFERENCES app_user(id),
    inspection_date       DATE            NOT NULL,
    score                 DECIMAL(5,2),
    grade                 VARCHAR(5),
    findings              JSONB,
    status                VARCHAR(30)     NOT NULL DEFAULT 'SCHEDULED',
    next_inspection_date  DATE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_safety_entity CHECK (entity_type IN ('RESTAURANT','HOME_CHEF','CLOUD_KITCHEN','COMMUNITY_KITCHEN','CATERER')),
    CONSTRAINT chk_food_safety_status CHECK (status IN ('SCHEDULED','IN_PROGRESS','PASSED','FAILED','FOLLOW_UP')),
    CONSTRAINT chk_food_safety_score CHECK (score IS NULL OR score BETWEEN 0 AND 100)
);

CREATE TABLE IF NOT EXISTS food_safety_violations (
    id                    BIGSERIAL       PRIMARY KEY,
    inspection_id         BIGINT          NOT NULL REFERENCES food_safety_inspections(id) ON DELETE CASCADE,
    violation_type        VARCHAR(50)     NOT NULL,
    severity              VARCHAR(20)     NOT NULL DEFAULT 'MINOR',
    description           VARCHAR(1000),
    corrective_action     VARCHAR(1000),
    resolved              BOOLEAN         NOT NULL DEFAULT FALSE,
    resolved_at           TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_violation_severity CHECK (severity IN ('MINOR','MAJOR','CRITICAL'))
);

CREATE TABLE IF NOT EXISTS food_safety_certifications (
    id                    BIGSERIAL       PRIMARY KEY,
    entity_type           VARCHAR(30)     NOT NULL,
    entity_id             BIGINT          NOT NULL,
    certification_type    VARCHAR(50)     NOT NULL,
    certificate_number    VARCHAR(100),
    issuing_authority     VARCHAR(200),
    issued_date           DATE,
    expiry_date           DATE,
    certificate_url       VARCHAR(500),
    status                VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_cert_status CHECK (status IN ('ACTIVE','EXPIRED','REVOKED','PENDING'))
);

CREATE TABLE IF NOT EXISTS food_safety_incident_reports (
    id                    BIGSERIAL       PRIMARY KEY,
    entity_type           VARCHAR(30)     NOT NULL,
    entity_id             BIGINT          NOT NULL,
    reported_by           BIGINT          NOT NULL REFERENCES app_user(id),
    incident_type         VARCHAR(50)     NOT NULL,
    description           VARCHAR(2000),
    severity              VARCHAR(20)     NOT NULL DEFAULT 'LOW',
    status                VARCHAR(30)     NOT NULL DEFAULT 'REPORTED',
    resolution            VARCHAR(1000),
    resolved_at           TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_incident_severity CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT chk_food_incident_status CHECK (status IN ('REPORTED','INVESTIGATING','RESOLVED','CLOSED'))
);


-- =====================================================================================
-- 26. FOOD SHARING & DONATION ENGINE (3 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_donations (
    id                    BIGSERIAL       PRIMARY KEY,
    donor_id              BIGINT          NOT NULL REFERENCES app_user(id),
    donor_type            VARCHAR(30)     NOT NULL DEFAULT 'INDIVIDUAL',
    item_name             VARCHAR(200)    NOT NULL,
    description           VARCHAR(500),
    quantity              DECIMAL(10,2),
    unit                  VARCHAR(30),
    pickup_address        VARCHAR(500),
    pickup_by             TIMESTAMP,
    status                VARCHAR(30)     NOT NULL DEFAULT 'AVAILABLE',
    claimed_by            BIGINT          REFERENCES app_user(id),
    claimed_at            TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_donation_status CHECK (status IN ('AVAILABLE','CLAIMED','PICKED_UP','EXPIRED','CANCELLED')),
    CONSTRAINT chk_food_donation_donor CHECK (donor_type IN ('INDIVIDUAL','RESTAURANT','COMMUNITY_KITCHEN','CATERER','CORPORATE'))
);

CREATE TABLE IF NOT EXISTS food_sharing_posts (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    title                 VARCHAR(200)    NOT NULL,
    description           VARCHAR(1000),
    food_type             VARCHAR(50),
    servings              INT,
    available_until       TIMESTAMP,
    pickup_location       VARCHAR(300),
    image_url             VARCHAR(500),
    is_veg                BOOLEAN         NOT NULL DEFAULT FALSE,
    status                VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_sharing_status CHECK (status IN ('ACTIVE','CLAIMED','EXPIRED','CANCELLED'))
);

CREATE TABLE IF NOT EXISTS food_sharing_claims (
    id                    BIGSERIAL       PRIMARY KEY,
    post_id               BIGINT          NOT NULL REFERENCES food_sharing_posts(id) ON DELETE CASCADE,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    servings_claimed      INT             NOT NULL DEFAULT 1,
    status                VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    picked_up_at          TIMESTAMP,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_food_sharing_claim_status CHECK (status IN ('PENDING','APPROVED','PICKED_UP','CANCELLED'))
);


-- =====================================================================================
-- 27. AUDIT & ACTIVITY LOG ENGINE (2 tables)
-- =====================================================================================

CREATE TABLE IF NOT EXISTS food_audit_logs (
    id                    BIGSERIAL       PRIMARY KEY,
    entity_type           VARCHAR(50)     NOT NULL,
    entity_id             BIGINT          NOT NULL,
    action                VARCHAR(30)     NOT NULL,
    actor_id              BIGINT          REFERENCES app_user(id),
    old_values            JSONB,
    new_values            JSONB,
    ip_address            VARCHAR(50),
    user_agent            VARCHAR(500),
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS food_activity_feeds (
    id                    BIGSERIAL       PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES app_user(id),
    activity_type         VARCHAR(50)     NOT NULL,
    title                 VARCHAR(200),
    description           VARCHAR(500),
    reference_type        VARCHAR(50),
    reference_id          BIGINT,
    image_url             VARCHAR(500),
    is_read               BOOLEAN         NOT NULL DEFAULT FALSE,
    community_id          BIGINT          NOT NULL REFERENCES community(id),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =====================================================================================
-- INDEXES
-- =====================================================================================

-- Resident Profiles
CREATE INDEX IF NOT EXISTS idx_food_resident_profiles_user ON food_resident_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_food_resident_profiles_community ON food_resident_profiles(community_id);
CREATE INDEX IF NOT EXISTS idx_food_resident_allergies_profile ON food_resident_allergies(profile_id);
CREATE INDEX IF NOT EXISTS idx_food_resident_med_restrict_profile ON food_resident_medical_restrictions(profile_id);
CREATE INDEX IF NOT EXISTS idx_food_resident_cuisine_pref_profile ON food_resident_cuisine_preferences(profile_id);
CREATE INDEX IF NOT EXISTS idx_food_resident_meal_timings_profile ON food_resident_meal_timings(profile_id);
CREATE INDEX IF NOT EXISTS idx_food_resident_favorites_profile ON food_resident_favorites(profile_id);
CREATE INDEX IF NOT EXISTS idx_food_resident_family_profile ON food_resident_family_members(profile_id);
CREATE INDEX IF NOT EXISTS idx_food_resident_goals_profile ON food_resident_goals(profile_id, status);

-- Restaurants
CREATE INDEX IF NOT EXISTS idx_food_restaurants_community ON food_restaurants(community_id, status);
CREATE INDEX IF NOT EXISTS idx_food_restaurants_slug ON food_restaurants(slug);
CREATE INDEX IF NOT EXISTS idx_food_restaurants_owner ON food_restaurants(owner_id);
CREATE INDEX IF NOT EXISTS idx_food_restaurants_status ON food_restaurants(status);
CREATE INDEX IF NOT EXISTS idx_food_restaurant_branches_rest ON food_restaurant_branches(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_food_restaurant_hours_rest ON food_restaurant_operating_hours(restaurant_id, day_of_week);
CREATE INDEX IF NOT EXISTS idx_food_menu_categories_rest ON food_menu_categories(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_food_menu_items_rest ON food_menu_items(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_food_menu_items_category ON food_menu_items(category_id);
CREATE INDEX IF NOT EXISTS idx_food_menu_items_slug ON food_menu_items(slug);
CREATE INDEX IF NOT EXISTS idx_food_menu_item_variants_item ON food_menu_item_variants(item_id);
CREATE INDEX IF NOT EXISTS idx_food_menu_item_addons_item ON food_menu_item_addons(item_id);
CREATE INDEX IF NOT EXISTS idx_food_menu_item_combos_rest ON food_menu_item_combos(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_food_menu_combo_items_combo ON food_menu_combo_items(combo_id);
CREATE INDEX IF NOT EXISTS idx_food_restaurant_tables_rest ON food_restaurant_tables(restaurant_id, status);
CREATE INDEX IF NOT EXISTS idx_food_restaurant_staff_rest ON food_restaurant_staff(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_food_restaurant_staff_user ON food_restaurant_staff(user_id);
CREATE INDEX IF NOT EXISTS idx_food_restaurant_reviews_rest ON food_restaurant_reviews(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_food_restaurant_reviews_user ON food_restaurant_reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_food_restaurant_offers_rest ON food_restaurant_offers(restaurant_id, active);
CREATE INDEX IF NOT EXISTS idx_food_restaurant_offers_coupon ON food_restaurant_offers(coupon_code);
CREATE INDEX IF NOT EXISTS idx_food_restaurant_offers_valid ON food_restaurant_offers(valid_from, valid_until);
CREATE INDEX IF NOT EXISTS idx_food_restaurant_analytics_rest ON food_restaurant_analytics(restaurant_id, date);
CREATE INDEX IF NOT EXISTS idx_food_restaurant_documents_rest ON food_restaurant_documents(restaurant_id);

-- Home Chefs
CREATE INDEX IF NOT EXISTS idx_food_home_chefs_community ON food_home_chefs(community_id, status);
CREATE INDEX IF NOT EXISTS idx_food_home_chefs_user ON food_home_chefs(user_id);
CREATE INDEX IF NOT EXISTS idx_food_home_chef_menu_chef ON food_home_chef_menu(chef_id, active);
CREATE INDEX IF NOT EXISTS idx_food_home_chef_certs_chef ON food_home_chef_certifications(chef_id);
CREATE INDEX IF NOT EXISTS idx_food_home_chef_hours_chef ON food_home_chef_operating_hours(chef_id, day_of_week);
CREATE INDEX IF NOT EXISTS idx_food_home_chef_reviews_chef ON food_home_chef_reviews(chef_id);
CREATE INDEX IF NOT EXISTS idx_food_home_chef_reviews_user ON food_home_chef_reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_food_home_chef_payouts_chef ON food_home_chef_payouts(chef_id, status);
CREATE INDEX IF NOT EXISTS idx_food_home_chef_specialties_chef ON food_home_chef_specialties(chef_id);
CREATE INDEX IF NOT EXISTS idx_food_home_chef_gallery_chef ON food_home_chef_gallery(chef_id);

-- Cloud Kitchens
CREATE INDEX IF NOT EXISTS idx_food_cloud_kitchens_community ON food_cloud_kitchens(community_id, status);
CREATE INDEX IF NOT EXISTS idx_food_cloud_kitchen_brands_kitchen ON food_cloud_kitchen_brands(kitchen_id);
CREATE INDEX IF NOT EXISTS idx_food_cloud_kitchen_slots_kitchen ON food_cloud_kitchen_slots(kitchen_id);
CREATE INDEX IF NOT EXISTS idx_food_cloud_kitchen_equipment_kitchen ON food_cloud_kitchen_equipment(kitchen_id);
CREATE INDEX IF NOT EXISTS idx_food_cloud_kitchen_prod_kitchen ON food_cloud_kitchen_production(kitchen_id, date);
CREATE INDEX IF NOT EXISTS idx_food_cloud_kitchen_analytics_kitchen ON food_cloud_kitchen_analytics(kitchen_id, date);

-- Community Kitchens
CREATE INDEX IF NOT EXISTS idx_food_comm_kitchens_community ON food_community_kitchens(community_id, status);
CREATE INDEX IF NOT EXISTS idx_food_comm_kitchen_menus_kitchen ON food_community_kitchen_menus(kitchen_id, date);
CREATE INDEX IF NOT EXISTS idx_food_comm_kitchen_bookings_menu ON food_community_kitchen_bookings(menu_id);
CREATE INDEX IF NOT EXISTS idx_food_comm_kitchen_bookings_user ON food_community_kitchen_bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_food_comm_kitchen_tokens_booking ON food_community_kitchen_tokens(booking_id);
CREATE INDEX IF NOT EXISTS idx_food_comm_kitchen_staff_kitchen ON food_community_kitchen_staff(kitchen_id);
CREATE INDEX IF NOT EXISTS idx_food_comm_kitchen_waste_kitchen ON food_community_kitchen_waste(kitchen_id, date);
CREATE INDEX IF NOT EXISTS idx_food_comm_kitchen_feedback_kitchen ON food_community_kitchen_feedback(kitchen_id, date);

-- Subscriptions
CREATE INDEX IF NOT EXISTS idx_food_sub_plans_community ON food_subscription_plans(community_id, active);
CREATE INDEX IF NOT EXISTS idx_food_sub_plans_provider ON food_subscription_plans(provider_type, provider_id);
CREATE INDEX IF NOT EXISTS idx_food_subscriptions_user ON food_subscriptions(user_id, status);
CREATE INDEX IF NOT EXISTS idx_food_subscriptions_plan ON food_subscriptions(plan_id);
CREATE INDEX IF NOT EXISTS idx_food_sub_deliveries_sub ON food_subscription_deliveries(subscription_id, date);
CREATE INDEX IF NOT EXISTS idx_food_sub_pauses_sub ON food_subscription_pauses(subscription_id);
CREATE INDEX IF NOT EXISTS idx_food_sub_customizations_sub ON food_subscription_customizations(subscription_id);
CREATE INDEX IF NOT EXISTS idx_food_sub_invoices_sub ON food_subscription_invoices(subscription_id);
CREATE INDEX IF NOT EXISTS idx_food_sub_invoices_number ON food_subscription_invoices(invoice_number);

-- Orders
CREATE INDEX IF NOT EXISTS idx_food_orders_user ON food_orders(user_id, status);
CREATE INDEX IF NOT EXISTS idx_food_orders_number ON food_orders(order_number);
CREATE INDEX IF NOT EXISTS idx_food_orders_provider ON food_orders(provider_type, provider_id);
CREATE INDEX IF NOT EXISTS idx_food_orders_community ON food_orders(community_id, status);
CREATE INDEX IF NOT EXISTS idx_food_orders_placed ON food_orders(placed_at DESC);
CREATE INDEX IF NOT EXISTS idx_food_orders_status ON food_orders(status);
CREATE INDEX IF NOT EXISTS idx_food_order_items_order ON food_order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_food_order_item_addons_item ON food_order_item_addons(order_item_id);
CREATE INDEX IF NOT EXISTS idx_food_order_tracking_order ON food_order_tracking(order_id);
CREATE INDEX IF NOT EXISTS idx_food_order_ratings_order ON food_order_ratings(order_id);
CREATE INDEX IF NOT EXISTS idx_food_order_ratings_user ON food_order_ratings(user_id);
CREATE INDEX IF NOT EXISTS idx_food_group_orders_created_by ON food_group_orders(created_by);
CREATE INDEX IF NOT EXISTS idx_food_group_orders_code ON food_group_orders(join_code);
CREATE INDEX IF NOT EXISTS idx_food_group_order_participants_group ON food_group_order_participants(group_order_id);
CREATE INDEX IF NOT EXISTS idx_food_order_refunds_order ON food_order_refunds(order_id);

-- Dining Reservations
CREATE INDEX IF NOT EXISTS idx_food_dining_res_restaurant ON food_dining_reservations(restaurant_id, date);
CREATE INDEX IF NOT EXISTS idx_food_dining_res_user ON food_dining_reservations(user_id);
CREATE INDEX IF NOT EXISTS idx_food_dining_res_status ON food_dining_reservations(status);
CREATE INDEX IF NOT EXISTS idx_food_dining_res_date ON food_dining_reservations(date);
CREATE INDEX IF NOT EXISTS idx_food_dining_waitlist_rest ON food_dining_waitlist(restaurant_id, status);
CREATE INDEX IF NOT EXISTS idx_food_dining_events_community ON food_dining_events(community_id, date);
CREATE INDEX IF NOT EXISTS idx_food_dining_events_status ON food_dining_events(status);
CREATE INDEX IF NOT EXISTS idx_food_dining_pre_orders_res ON food_dining_pre_orders(reservation_id);
CREATE INDEX IF NOT EXISTS idx_food_dining_feedback_res ON food_dining_feedback(reservation_id);

-- Delivery
CREATE INDEX IF NOT EXISTS idx_food_del_partners_community ON food_delivery_partners(community_id, status);
CREATE INDEX IF NOT EXISTS idx_food_del_partners_user ON food_delivery_partners(user_id);
CREATE INDEX IF NOT EXISTS idx_food_del_assignments_order ON food_delivery_assignments(order_id);
CREATE INDEX IF NOT EXISTS idx_food_del_assignments_partner ON food_delivery_assignments(partner_id, status);
CREATE INDEX IF NOT EXISTS idx_food_del_zones_community ON food_delivery_zones(community_id, active);
CREATE INDEX IF NOT EXISTS idx_food_del_lockers_community ON food_delivery_lockers(community_id);
CREATE INDEX IF NOT EXISTS idx_food_del_locker_assign_locker ON food_delivery_locker_assignments(locker_id);
CREATE INDEX IF NOT EXISTS idx_food_del_locker_assign_order ON food_delivery_locker_assignments(order_id);
CREATE INDEX IF NOT EXISTS idx_food_del_route_logs_assign ON food_delivery_route_logs(assignment_id);

-- Grocery
CREATE INDEX IF NOT EXISTS idx_food_grocery_stores_community ON food_grocery_stores(community_id, status);
CREATE INDEX IF NOT EXISTS idx_food_grocery_stores_slug ON food_grocery_stores(slug);
CREATE INDEX IF NOT EXISTS idx_food_grocery_categories_store ON food_grocery_categories(store_id);
CREATE INDEX IF NOT EXISTS idx_food_grocery_products_store ON food_grocery_products(store_id);
CREATE INDEX IF NOT EXISTS idx_food_grocery_products_category ON food_grocery_products(category_id);
CREATE INDEX IF NOT EXISTS idx_food_grocery_products_slug ON food_grocery_products(slug);
CREATE INDEX IF NOT EXISTS idx_food_grocery_products_barcode ON food_grocery_products(barcode);
CREATE INDEX IF NOT EXISTS idx_food_grocery_orders_user ON food_grocery_orders(user_id, status);
CREATE INDEX IF NOT EXISTS idx_food_grocery_orders_store ON food_grocery_orders(store_id);
CREATE INDEX IF NOT EXISTS idx_food_grocery_orders_number ON food_grocery_orders(order_number);
CREATE INDEX IF NOT EXISTS idx_food_grocery_order_items_order ON food_grocery_order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_food_grocery_del_slots_store ON food_grocery_delivery_slots(store_id, date);
CREATE INDEX IF NOT EXISTS idx_food_grocery_wishlists_user ON food_grocery_wishlists(user_id);
CREATE INDEX IF NOT EXISTS idx_food_grocery_wishlists_product ON food_grocery_wishlists(product_id);

-- Kitchen Inventory
CREATE INDEX IF NOT EXISTS idx_food_kitchen_inv_kitchen ON food_kitchen_inventory(kitchen_type, kitchen_id);
CREATE INDEX IF NOT EXISTS idx_food_kitchen_inv_community ON food_kitchen_inventory(community_id);
CREATE INDEX IF NOT EXISTS idx_food_kitchen_inv_expiry ON food_kitchen_inventory(expiry_date);
CREATE INDEX IF NOT EXISTS idx_food_kitchen_inv_txn_inv ON food_kitchen_inventory_transactions(inventory_id);
CREATE INDEX IF NOT EXISTS idx_food_kitchen_suppliers_community ON food_kitchen_suppliers(community_id, active);
CREATE INDEX IF NOT EXISTS idx_food_kitchen_po_supplier ON food_kitchen_purchase_orders(supplier_id);
CREATE INDEX IF NOT EXISTS idx_food_kitchen_po_number ON food_kitchen_purchase_orders(order_number);
CREATE INDEX IF NOT EXISTS idx_food_kitchen_po_status ON food_kitchen_purchase_orders(status);
CREATE INDEX IF NOT EXISTS idx_food_kitchen_po_items_order ON food_kitchen_purchase_order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_food_kitchen_waste_kitchen ON food_kitchen_waste_logs(kitchen_type, kitchen_id, date);
CREATE INDEX IF NOT EXISTS idx_food_kitchen_forecast_kitchen ON food_kitchen_inventory_forecasts(kitchen_type, kitchen_id, forecast_date);

-- Recipes
CREATE INDEX IF NOT EXISTS idx_food_recipes_community ON food_recipes(community_id, status);
CREATE INDEX IF NOT EXISTS idx_food_recipes_slug ON food_recipes(slug);
CREATE INDEX IF NOT EXISTS idx_food_recipes_author ON food_recipes(author_id);
CREATE INDEX IF NOT EXISTS idx_food_recipes_cuisine ON food_recipes(cuisine_type);
CREATE INDEX IF NOT EXISTS idx_food_recipes_meal ON food_recipes(meal_type);
CREATE INDEX IF NOT EXISTS idx_food_recipe_ingredients_recipe ON food_recipe_ingredients(recipe_id);
CREATE INDEX IF NOT EXISTS idx_food_recipe_collections_user ON food_recipe_collections(user_id);
CREATE INDEX IF NOT EXISTS idx_food_recipe_collection_items_coll ON food_recipe_collection_items(collection_id);
CREATE INDEX IF NOT EXISTS idx_food_recipe_comments_recipe ON food_recipe_comments(recipe_id);
CREATE INDEX IF NOT EXISTS idx_food_recipe_ratings_recipe ON food_recipe_ratings(recipe_id);
CREATE INDEX IF NOT EXISTS idx_food_recipe_ratings_user ON food_recipe_ratings(user_id);

-- Nutrition
CREATE INDEX IF NOT EXISTS idx_food_nutritionists_community ON food_nutritionists(community_id, status);
CREATE INDEX IF NOT EXISTS idx_food_nutritionists_user ON food_nutritionists(user_id);
CREATE INDEX IF NOT EXISTS idx_food_consultations_nutritionist ON food_nutrition_consultations(nutritionist_id, status);
CREATE INDEX IF NOT EXISTS idx_food_consultations_user ON food_nutrition_consultations(user_id);
CREATE INDEX IF NOT EXISTS idx_food_consultations_scheduled ON food_nutrition_consultations(scheduled_at);
CREATE INDEX IF NOT EXISTS idx_food_meal_plans_user ON food_meal_plans(user_id, status);
CREATE INDEX IF NOT EXISTS idx_food_meal_plan_items_plan ON food_meal_plan_items(plan_id);
CREATE INDEX IF NOT EXISTS idx_food_calorie_logs_user ON food_calorie_logs(user_id, date);
CREATE INDEX IF NOT EXISTS idx_food_weight_logs_user ON food_weight_logs(user_id, date);
CREATE INDEX IF NOT EXISTS idx_food_water_logs_user ON food_water_logs(user_id, date);

-- Events
CREATE INDEX IF NOT EXISTS idx_food_events_community ON food_events(community_id, status);
CREATE INDEX IF NOT EXISTS idx_food_events_date ON food_events(date);
CREATE INDEX IF NOT EXISTS idx_food_events_type ON food_events(event_type);
CREATE INDEX IF NOT EXISTS idx_food_event_registrations_event ON food_event_registrations(event_id);
CREATE INDEX IF NOT EXISTS idx_food_event_registrations_user ON food_event_registrations(user_id);
CREATE INDEX IF NOT EXISTS idx_food_event_contributions_event ON food_event_contributions(event_id);
CREATE INDEX IF NOT EXISTS idx_food_event_feedback_event ON food_event_feedback(event_id);
CREATE INDEX IF NOT EXISTS idx_food_event_sponsors_event ON food_event_sponsors(event_id);

-- Corporate
CREATE INDEX IF NOT EXISTS idx_food_corp_accounts_community ON food_corporate_accounts(community_id, status);
CREATE INDEX IF NOT EXISTS idx_food_corp_meal_cards_account ON food_corporate_meal_cards(account_id);
CREATE INDEX IF NOT EXISTS idx_food_corp_meal_cards_user ON food_corporate_meal_cards(user_id);
CREATE INDEX IF NOT EXISTS idx_food_corp_meal_cards_number ON food_corporate_meal_cards(card_number);
CREATE INDEX IF NOT EXISTS idx_food_corp_meal_card_txn_card ON food_corporate_meal_card_transactions(card_id);
CREATE INDEX IF NOT EXISTS idx_food_corp_cafeterias_account ON food_corporate_cafeterias(account_id);
CREATE INDEX IF NOT EXISTS idx_food_corp_caf_menus_cafeteria ON food_corporate_cafeteria_menus(cafeteria_id, date);
CREATE INDEX IF NOT EXISTS idx_food_corp_catering_account ON food_corporate_catering_requests(account_id, status);

-- Catering
CREATE INDEX IF NOT EXISTS idx_food_caterers_community ON food_caterers(community_id, status);
CREATE INDEX IF NOT EXISTS idx_food_catering_packages_caterer ON food_catering_packages(caterer_id, active);
CREATE INDEX IF NOT EXISTS idx_food_catering_requests_community ON food_catering_requests(community_id, status);
CREATE INDEX IF NOT EXISTS idx_food_catering_requests_user ON food_catering_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_food_catering_quotations_request ON food_catering_quotations(request_id);
CREATE INDEX IF NOT EXISTS idx_food_catering_quotations_caterer ON food_catering_quotations(caterer_id);
CREATE INDEX IF NOT EXISTS idx_food_catering_orders_caterer ON food_catering_orders(caterer_id, status);
CREATE INDEX IF NOT EXISTS idx_food_catering_orders_number ON food_catering_orders(order_number);
CREATE INDEX IF NOT EXISTS idx_food_catering_reviews_order ON food_catering_reviews(order_id);

-- AI
CREATE INDEX IF NOT EXISTS idx_food_ai_rec_user ON food_ai_recommendations(user_id, community_id);
CREATE INDEX IF NOT EXISTS idx_food_ai_rec_type ON food_ai_recommendations(recommendation_type, status);
CREATE INDEX IF NOT EXISTS idx_food_ai_meal_plans_user ON food_ai_meal_plans(user_id);
CREATE INDEX IF NOT EXISTS idx_food_ai_grocery_lists_user ON food_ai_grocery_lists(user_id);
CREATE INDEX IF NOT EXISTS idx_food_ai_demand_provider ON food_ai_demand_predictions(provider_type, provider_id, prediction_date);

-- Pantry
CREATE INDEX IF NOT EXISTS idx_food_pantry_items_user ON food_pantry_items(user_id, status);
CREATE INDEX IF NOT EXISTS idx_food_pantry_items_expiry ON food_pantry_items(expiry_date);
CREATE INDEX IF NOT EXISTS idx_food_pantry_items_community ON food_pantry_items(community_id);
CREATE INDEX IF NOT EXISTS idx_food_pantry_consumption_item ON food_pantry_consumption_logs(pantry_item_id);
CREATE INDEX IF NOT EXISTS idx_food_pantry_shopping_user ON food_pantry_shopping_lists(user_id, status);
CREATE INDEX IF NOT EXISTS idx_food_pantry_shopping_items_list ON food_pantry_shopping_list_items(list_id);
CREATE INDEX IF NOT EXISTS idx_food_pantry_alerts_user ON food_pantry_alerts(user_id, is_read);

-- Loyalty
CREATE INDEX IF NOT EXISTS idx_food_loyalty_programs_community ON food_loyalty_programs(community_id, status);
CREATE INDEX IF NOT EXISTS idx_food_loyalty_members_program ON food_loyalty_members(program_id);
CREATE INDEX IF NOT EXISTS idx_food_loyalty_members_user ON food_loyalty_members(user_id);
CREATE INDEX IF NOT EXISTS idx_food_loyalty_txn_member ON food_loyalty_transactions(member_id);
CREATE INDEX IF NOT EXISTS idx_food_loyalty_coupons_code ON food_loyalty_coupons(code);
CREATE INDEX IF NOT EXISTS idx_food_loyalty_coupons_community ON food_loyalty_coupons(community_id, active);
CREATE INDEX IF NOT EXISTS idx_food_loyalty_coupon_usages_coupon ON food_loyalty_coupon_usages(coupon_id);
CREATE INDEX IF NOT EXISTS idx_food_loyalty_coupon_usages_user ON food_loyalty_coupon_usages(user_id);
CREATE INDEX IF NOT EXISTS idx_food_loyalty_gift_cards_number ON food_loyalty_gift_cards(card_number);
CREATE INDEX IF NOT EXISTS idx_food_loyalty_gift_cards_community ON food_loyalty_gift_cards(community_id, status);

-- Reviews
CREATE INDEX IF NOT EXISTS idx_food_reviews_entity ON food_reviews(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_food_reviews_user ON food_reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_food_reviews_community ON food_reviews(community_id, status);
CREATE INDEX IF NOT EXISTS idx_food_review_responses_review ON food_review_responses(review_id);
CREATE INDEX IF NOT EXISTS idx_food_review_reports_review ON food_review_reports(review_id, status);

-- Payments
CREATE INDEX IF NOT EXISTS idx_food_payments_user ON food_payments(user_id);
CREATE INDEX IF NOT EXISTS idx_food_payments_order ON food_payments(order_type, order_id);
CREATE INDEX IF NOT EXISTS idx_food_payments_status ON food_payments(status);
CREATE INDEX IF NOT EXISTS idx_food_payments_community ON food_payments(community_id);
CREATE INDEX IF NOT EXISTS idx_food_payments_txn ON food_payments(transaction_id);
CREATE INDEX IF NOT EXISTS idx_food_wallets_user ON food_wallets(user_id, community_id);
CREATE INDEX IF NOT EXISTS idx_food_wallet_txn_wallet ON food_wallet_transactions(wallet_id);

-- Notifications
CREATE INDEX IF NOT EXISTS idx_food_notif_pref_user ON food_notification_preferences(user_id, community_id);
CREATE INDEX IF NOT EXISTS idx_food_notif_logs_user ON food_notification_logs(user_id, community_id);
CREATE INDEX IF NOT EXISTS idx_food_notif_logs_sent ON food_notification_logs(sent_at DESC);
CREATE INDEX IF NOT EXISTS idx_food_notif_logs_status ON food_notification_logs(status);

-- Workflows
CREATE INDEX IF NOT EXISTS idx_food_workflow_def_community ON food_workflow_definitions(community_id, active);
CREATE INDEX IF NOT EXISTS idx_food_workflow_inst_def ON food_workflow_instances(definition_id);
CREATE INDEX IF NOT EXISTS idx_food_workflow_inst_entity ON food_workflow_instances(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_food_workflow_inst_status ON food_workflow_instances(status);

-- Analytics
CREATE INDEX IF NOT EXISTS idx_food_analytics_daily_entity ON food_analytics_daily(entity_type, entity_id, date);
CREATE INDEX IF NOT EXISTS idx_food_analytics_daily_community ON food_analytics_daily(community_id, date);
CREATE INDEX IF NOT EXISTS idx_food_analytics_trends_community ON food_analytics_consumption_trends(community_id, month);
CREATE INDEX IF NOT EXISTS idx_food_analytics_waste_community ON food_analytics_food_waste(community_id, month);

-- Food Safety
CREATE INDEX IF NOT EXISTS idx_food_safety_inspections_entity ON food_safety_inspections(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_food_safety_inspections_date ON food_safety_inspections(inspection_date);
CREATE INDEX IF NOT EXISTS idx_food_safety_inspections_community ON food_safety_inspections(community_id, status);
CREATE INDEX IF NOT EXISTS idx_food_safety_violations_inspection ON food_safety_violations(inspection_id);
CREATE INDEX IF NOT EXISTS idx_food_safety_certs_entity ON food_safety_certifications(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_food_safety_certs_expiry ON food_safety_certifications(expiry_date);
CREATE INDEX IF NOT EXISTS idx_food_safety_incidents_entity ON food_safety_incident_reports(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_food_safety_incidents_community ON food_safety_incident_reports(community_id, status);

-- Food Sharing & Donations
CREATE INDEX IF NOT EXISTS idx_food_donations_community ON food_donations(community_id, status);
CREATE INDEX IF NOT EXISTS idx_food_donations_donor ON food_donations(donor_id);
CREATE INDEX IF NOT EXISTS idx_food_sharing_posts_community ON food_sharing_posts(community_id, status);
CREATE INDEX IF NOT EXISTS idx_food_sharing_posts_user ON food_sharing_posts(user_id);
CREATE INDEX IF NOT EXISTS idx_food_sharing_claims_post ON food_sharing_claims(post_id);
CREATE INDEX IF NOT EXISTS idx_food_sharing_claims_user ON food_sharing_claims(user_id);

-- Audit & Activity
CREATE INDEX IF NOT EXISTS idx_food_audit_logs_entity ON food_audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_food_audit_logs_community ON food_audit_logs(community_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_food_audit_logs_actor ON food_audit_logs(actor_id);
CREATE INDEX IF NOT EXISTS idx_food_activity_feeds_user ON food_activity_feeds(user_id, community_id);
CREATE INDEX IF NOT EXISTS idx_food_activity_feeds_created ON food_activity_feeds(created_at DESC);


-- =====================================================================================
-- UNIQUE CONSTRAINTS
-- =====================================================================================

CREATE UNIQUE INDEX IF NOT EXISTS uq_food_resident_profiles_user_community ON food_resident_profiles(user_id, community_id) WHERE deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_food_restaurants_slug_community ON food_restaurants(slug, community_id) WHERE slug IS NOT NULL AND deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_food_restaurant_hours ON food_restaurant_operating_hours(restaurant_id, day_of_week);
CREATE UNIQUE INDEX IF NOT EXISTS uq_food_menu_items_slug_rest ON food_menu_items(slug, restaurant_id) WHERE slug IS NOT NULL AND deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_food_restaurant_staff ON food_restaurant_staff(restaurant_id, user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_food_home_chefs_user ON food_home_chefs(user_id, community_id) WHERE deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_food_home_chef_hours ON food_home_chef_operating_hours(chef_id, day_of_week);
CREATE UNIQUE INDEX IF NOT EXISTS uq_food_grocery_stores_slug ON food_grocery_stores(slug, community_id) WHERE slug IS NOT NULL AND deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_food_grocery_products_slug ON food_grocery_products(slug, store_id) WHERE slug IS NOT NULL AND deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_food_grocery_wishlists ON food_grocery_wishlists(user_id, product_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_food_recipe_ratings ON food_recipe_ratings(recipe_id, user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_food_loyalty_members ON food_loyalty_members(program_id, user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_food_wallets_user_community ON food_wallets(user_id, community_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_food_notif_pref_user_community ON food_notification_preferences(user_id, community_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_food_analytics_daily ON food_analytics_daily(entity_type, entity_id, date, metric_name);
CREATE UNIQUE INDEX IF NOT EXISTS uq_food_analytics_trends ON food_analytics_consumption_trends(community_id, month);
CREATE UNIQUE INDEX IF NOT EXISTS uq_food_analytics_waste ON food_analytics_food_waste(community_id, month);
CREATE UNIQUE INDEX IF NOT EXISTS uq_food_restaurant_analytics ON food_restaurant_analytics(restaurant_id, date);
CREATE UNIQUE INDEX IF NOT EXISTS uq_food_cloud_kitchen_analytics ON food_cloud_kitchen_analytics(kitchen_id, date);
