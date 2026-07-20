-- FILE: db/sql/v1.0.0/30_marketplace_category.sql
-- ═══════════════════════════════════════════════════════════════════════════
-- MARKETPLACE MODULE — Category hierarchy
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS marketplace_category (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(60)     NOT NULL,
    slug            VARCHAR(60)     NOT NULL,
    icon            VARCHAR(60),
    parent_id       BIGINT          REFERENCES marketplace_category(id),
    sort_order      INT             NOT NULL DEFAULT 0,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    community_id    BIGINT          REFERENCES community(id),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_mkt_category_slug
    ON marketplace_category (slug, COALESCE(community_id, 0));
