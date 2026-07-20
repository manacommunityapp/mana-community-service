-- FILE: db/sql/v1.0.0/29_marketplace_listing.sql
-- ═══════════════════════════════════════════════════════════════════════════
-- MARKETPLACE MODULE — Listing & Listing Image tables
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS listing (
    id                BIGSERIAL       PRIMARY KEY,
    title             VARCHAR(150)    NOT NULL,
    description       VARCHAR(2000),
    price             DECIMAL(12,2)   NOT NULL,
    price_unit        VARCHAR(20),
    category          VARCHAR(40)     NOT NULL,
    status            VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    transaction_mode  VARCHAR(20),
    visibility        VARCHAR(20)     DEFAULT 'COMMUNITY',
    location          VARCHAR(100),
    seller_id         BIGINT          NOT NULL REFERENCES app_user(id),
    community_id      BIGINT          NOT NULL REFERENCES community(id),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS listing_image (
    id                BIGSERIAL       PRIMARY KEY,
    listing_id        BIGINT          NOT NULL REFERENCES listing(id) ON DELETE CASCADE,
    url               VARCHAR(500)    NOT NULL,
    sort_order        INT             NOT NULL DEFAULT 0,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── Indexes ──────────────────────────────────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_listing_community_status
    ON listing (community_id, status);

CREATE INDEX IF NOT EXISTS idx_listing_community_category_status
    ON listing (community_id, status, category);

CREATE INDEX IF NOT EXISTS idx_listing_seller
    ON listing (seller_id);

CREATE INDEX IF NOT EXISTS idx_listing_created_at
    ON listing (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_listing_image_listing
    ON listing_image (listing_id, sort_order);
