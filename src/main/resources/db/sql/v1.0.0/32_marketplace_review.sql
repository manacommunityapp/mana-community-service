-- FILE: db/sql/v1.0.0/32_marketplace_review.sql
-- ═══════════════════════════════════════════════════════════════════════════
-- MARKETPLACE MODULE — Ratings & Reviews
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS listing_review (
    id              BIGSERIAL       PRIMARY KEY,
    listing_id      BIGINT          NOT NULL REFERENCES listing(id) ON DELETE CASCADE,
    reviewer_id     BIGINT          NOT NULL REFERENCES app_user(id),
    rating          SMALLINT        NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment         VARCHAR(1000),
    seller_reply    VARCHAR(1000),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_review_listing_reviewer
    ON listing_review (listing_id, reviewer_id);

CREATE INDEX IF NOT EXISTS idx_review_listing
    ON listing_review (listing_id);
