-- FILE: db/sql/v1.0.0/31_marketplace_wishlist.sql
-- ═══════════════════════════════════════════════════════════════════════════
-- MARKETPLACE MODULE — Wishlist
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS wishlist (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES app_user(id),
    listing_id      BIGINT          NOT NULL REFERENCES listing(id) ON DELETE CASCADE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_wishlist_user_listing
    ON wishlist (user_id, listing_id);
