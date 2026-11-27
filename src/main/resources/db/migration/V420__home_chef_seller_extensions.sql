-- Extend existing market_seller_profiles with home chef / community seller features
ALTER TABLE market_seller_profiles
    ADD COLUMN IF NOT EXISTS operating_hours   JSONB,
    ADD COLUMN IF NOT EXISTS whatsapp_contact  VARCHAR(20),
    ADD COLUMN IF NOT EXISTS seller_category   VARCHAR(50),  -- HOME_CHEF, TUTOR, TAILOR, BEAUTY, PET_SERVICES, OTHER
    ADD COLUMN IF NOT EXISTS is_verified_chef  BOOLEAN NOT NULL DEFAULT FALSE;

-- Community-specific seller listings (slot-based or catalogue items)
CREATE TABLE IF NOT EXISTS community_seller_listings (
    id               BIGSERIAL PRIMARY KEY,
    seller_id        BIGINT NOT NULL REFERENCES market_seller_profiles(id) ON DELETE CASCADE,
    community_id     BIGINT NOT NULL,
    title            VARCHAR(300) NOT NULL,
    description      TEXT,
    category         VARCHAR(100),    -- Breakfast, Lunch, Tiffin, Maths, English, etc.
    price            NUMERIC(10,2),
    price_unit       VARCHAR(50),     -- per meal, per hour, per session, per piece
    images           JSONB,           -- [{url, caption}]
    is_available     BOOLEAN NOT NULL DEFAULT TRUE,
    slot_based       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seller reviews (extends existing market reviews with community scoping)
CREATE TABLE IF NOT EXISTS community_seller_reviews (
    id               BIGSERIAL PRIMARY KEY,
    seller_id        BIGINT NOT NULL REFERENCES market_seller_profiles(id) ON DELETE CASCADE,
    reviewer_id      BIGINT NOT NULL,
    community_id     BIGINT NOT NULL,
    rating           SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment          TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (seller_id, reviewer_id)
);

CREATE INDEX IF NOT EXISTS idx_csl_community   ON community_seller_listings(community_id, is_available);
CREATE INDEX IF NOT EXISTS idx_csl_seller      ON community_seller_listings(seller_id);
CREATE INDEX IF NOT EXISTS idx_csr_seller      ON community_seller_reviews(seller_id);
CREATE INDEX IF NOT EXISTS idx_msp_category    ON market_seller_profiles(community_id, seller_category, is_active);
