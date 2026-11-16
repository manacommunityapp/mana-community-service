-- Jewellery Virtual Try-On module
-- V241

CREATE TABLE jewellery_vendor_profile (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT NOT NULL REFERENCES app_user(id),
    shop_name            VARCHAR(255) NOT NULL,
    owner_name           VARCHAR(255) NOT NULL,
    phone_number         VARCHAR(15) NOT NULL,
    email                VARCHAR(255),
    address              TEXT NOT NULL,
    service_areas        TEXT,
    gst_number           VARCHAR(20),
    pan_number           VARCHAR(12),
    bis_hallmark_license VARCHAR(50),
    specialties          TEXT,
    image_url            TEXT,
    rating               NUMERIC(3, 2) DEFAULT 0.0,
    review_count         INT DEFAULT 0,
    open_hours           VARCHAR(100),
    verification_status  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    verified_at          TIMESTAMP,
    is_active            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(255),
    updated_by           VARCHAR(255),
    UNIQUE (user_id)
);

CREATE TABLE jewellery_product (
    id                BIGSERIAL PRIMARY KEY,
    vendor_id         BIGINT NOT NULL REFERENCES jewellery_vendor_profile(id),
    sku               VARCHAR(100),
    name              VARCHAR(255) NOT NULL,
    category          VARCHAR(50) NOT NULL,
    jewellery_type    VARCHAR(50),
    material          VARCHAR(50) NOT NULL,
    purity            VARCHAR(30),
    net_weight_g      NUMERIC(10, 3),
    description       TEXT,
    huid              VARCHAR(6),
    tags              TEXT,
    gold_rate_per_gram NUMERIC(12, 2),
    making_charges    NUMERIC(12, 2),
    stone_cost        NUMERIC(12, 2),
    total_price       NUMERIC(12, 2) NOT NULL,
    gst_percent       NUMERIC(5, 2) DEFAULT 3.0,
    stock_quantity    INT NOT NULL DEFAULT 0,
    stone_details     JSONB,
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255)
);

CREATE TABLE jewellery_product_asset (
    id                  BIGSERIAL PRIMARY KEY,
    product_id          BIGINT NOT NULL REFERENCES jewellery_product(id),
    asset_type          VARCHAR(30) NOT NULL,
    asset_url           TEXT NOT NULL,
    body_part           VARCHAR(20),
    format              VARCHAR(20),
    asset_version       INT NOT NULL DEFAULT 1,
    approval_status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    placement_metadata  JSONB,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);

CREATE TABLE jewellery_tryon_session (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES app_user(id),
    product_id BIGINT REFERENCES jewellery_product(id),
    provider   VARCHAR(50),
    mode       VARCHAR(20) NOT NULL,
    platform   VARCHAR(20) NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'STARTED',
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ended_at   TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE TABLE jewellery_photo_job (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES app_user(id),
    product_id        BIGINT REFERENCES jewellery_product(id),
    status            VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    source_object_key TEXT NOT NULL,
    output_object_key TEXT,
    provider          VARCHAR(50),
    user_saved        BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at        TIMESTAMP NOT NULL,
    error_code        VARCHAR(100),
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at      TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255)
);

CREATE TABLE jewellery_wishlist (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES app_user(id),
    product_id BIGINT NOT NULL REFERENCES jewellery_product(id),
    saved_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    UNIQUE (user_id, product_id)
);

CREATE TABLE jewellery_appointment (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES app_user(id),
    vendor_id           BIGINT NOT NULL REFERENCES jewellery_vendor_profile(id),
    appointment_date    DATE NOT NULL,
    appointment_time    VARCHAR(10) NOT NULL,
    purpose             VARCHAR(30) NOT NULL,
    notes               TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    cancellation_reason TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);

-- Indexes
CREATE INDEX idx_jvp_user         ON jewellery_vendor_profile(user_id);
CREATE INDEX idx_jvp_active       ON jewellery_vendor_profile(is_active, rating DESC);
CREATE INDEX idx_jp_vendor        ON jewellery_product(vendor_id);
CREATE INDEX idx_jp_status        ON jewellery_product(status);
CREATE INDEX idx_jp_category      ON jewellery_product(category, status);
CREATE INDEX idx_jp_material      ON jewellery_product(material, status);
CREATE INDEX idx_jpa_product      ON jewellery_product_asset(product_id, asset_type, approval_status);
CREATE INDEX idx_jts_user         ON jewellery_tryon_session(user_id);
CREATE INDEX idx_jpj_user         ON jewellery_photo_job(user_id);
CREATE INDEX idx_jpj_expires      ON jewellery_photo_job(expires_at) WHERE status NOT IN ('DELETION_PENDING','EXPIRED');
CREATE INDEX idx_jw_user          ON jewellery_wishlist(user_id);
CREATE INDEX idx_ja_user          ON jewellery_appointment(user_id, appointment_date DESC);
CREATE INDEX idx_ja_vendor_status ON jewellery_appointment(vendor_id, status);
